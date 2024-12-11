package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.ILineFinder;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LineFinder implements ILineFinder {

    private final int THREADS_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int BUFF_SIZE = 1024;

    private Set<Long> processFile(String filePath, String searchKeyword, int maxLines,
                                  ExecutorService threadPool) throws Exception {
        String searchKeywordLower = searchKeyword.toLowerCase();
        long totalFileSize = getFileSize(filePath);
        List<List<Long>> fileBlocks = createFileBlocks(totalFileSize);

        AtomicInteger progressCounter = new AtomicInteger(0);
        AtomicInteger matchCounter = new AtomicInteger(0);
        List<Future<Set<Long>>> futureResults = new ArrayList<>();

        for (int threadIndex = 0; threadIndex < THREADS_COUNT; threadIndex++) {
            futureResults.add(threadPool.submit(() -> processFileBlock(fileBlocks, filePath, searchKeywordLower, progressCounter, matchCounter, maxLines)));
        }

        return consolidateResults(futureResults);
    }

    private List<List<Long>> createFileBlocks(long totalSize) {
        List<List<Long>> blocks = new ArrayList<>();

        for (long position = 0; position < totalSize; position += BUFF_SIZE) {
            long blockEnd = Math.min(position + BUFF_SIZE, totalSize);
            blocks.add(List.of(position, blockEnd));
        }

        return blocks;
    }

    private Set<Long> processFileBlock(List<List<Long>> blocks, String filePath, String searchKeyword,
                                       AtomicInteger progress, AtomicInteger matchedCount, int maxLines) throws Exception {
        Set<Long> foundLinePositions = new TreeSet<>();

        for (List<Long> block : blocks) {
            long startPosition = block.get(0);
            long endPosition = block.get(1);
            Set<Long> currentLines = findKeywordInLines(progress, matchedCount, filePath, searchKeyword, startPosition, endPosition, maxLines);
            foundLinePositions.addAll(currentLines);
        }

        synchronized (System.out) {
            printProgress(progress.get(), matchedCount.get());
        }

        return foundLinePositions;
    }

    private void printProgress(int processed, int matchesFound) {
        System.out.printf("Processed: %d, matches found %d\n", processed,  matchesFound);
    }

    private Set<Long> consolidateResults(List<Future<Set<Long>>> futures) throws Exception {
        Set<Long> aggregatedResults = new TreeSet<>();

        for (Future<Set<Long>> future : futures) {
            try {
                aggregatedResults.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new Exception("Error combining results: " + e.getMessage(), e);
            }
        }

        return aggregatedResults;
    }

    private Set<Long> findKeywordInLines(AtomicInteger progressTracker, AtomicInteger matchCounter,
                                          String path, String searchTerm, long startOffset, long endOffset, int limitLines) throws Exception {
        Set<Long> matchedLines = new TreeSet<>();

        try (RandomAccessFile accessFile = new RandomAccessFile(path, "r")) {
            accessFile.seek(startOffset);
            if (startOffset != 0) {
                accessFile.readLine();
            }

            List<Long> lineOffsets = new ArrayList<>();

            while (accessFile.getFilePointer() < endOffset) {
                long currentOffset = accessFile.getFilePointer();
                String lineContent = accessFile.readLine();
                String decodedLine = lineContent != null ? new String(lineContent.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8) : "";

                if (!decodedLine.isEmpty()) {
                    lineOffsets.add(currentOffset);
                    if (decodedLine.toLowerCase().contains(searchTerm)) {
                        matchCounter.incrementAndGet();
                        Set<Long> positions = addResultInSet(limitLines, lineOffsets, accessFile, currentOffset);
                        matchedLines.addAll(positions);
                    }
                    progressTracker.incrementAndGet();
                }
            }
        } catch (IOException e) {
            throw new Exception("error: " + e.getMessage(), e);
        }

        return matchedLines;
    }

    private long getFileSize(String filename) throws Exception {
        try {
            File file = new File(filename);
            return file.length();
        } catch (SecurityException e) {
            throw new Exception("Access to the file is denied: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new Exception("An error occurred while accessing the file: " + e.getMessage(), e);
        }
    }

    private Set<Long> addResultInSet(int limit, List<Long> recordedPositions, RandomAccessFile accessFile, long currentPosition) throws IOException {
        Set<Long> accumulatedResults = new TreeSet<>();

        for (int index = recordedPositions.size() - 1; index >= Math.max(0, recordedPositions.size() - limit - 1); index--) {
            accumulatedResults.add(recordedPositions.get(index));
        }

        accumulatedResults.add(currentPosition);

        long filePointer = accessFile.getFilePointer();

        for (int index = 0; index < limit; index++) {
            if (accessFile.readLine() != null) {
                accumulatedResults.add(filePointer);
                filePointer = accessFile.getFilePointer();
            }
        }

        accessFile.seek(filePointer);

        return accumulatedResults;
    }

    private void writeInformationInFile(String filename, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to write information in file: " + e.getMessage(), e);
        }
    }

    private void writeInformationInEmptyFile(String file) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(" ");
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to write information in file: " + e.getMessage(), e);
        }
    }

    private List<String> doStringsTogether(String filename, Set<Long> positions) throws Exception {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            for (Long start : positions) {
                file.seek(start);
                String scan = file.readLine();
                String line = scan != null ? new String(scan.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8) : "";
                if (!line.isEmpty()) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            throw new Exception("Error with read file: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void find(String inputFilename, String outputFilename, String keyWord, int lineCount) throws LineCountShouldBePositiveException {
        if (lineCount < 0) {
            throw new LineCountShouldBePositiveException("Line count should be positive.");
        }
        if (keyWord == null || keyWord.isEmpty()) {
            writeInformationInEmptyFile(outputFilename);
            return;
        }
        if (inputFilename == null || inputFilename.isEmpty()) {
            throw new IllegalArgumentException("Input filename shouldn't be empty.");
        }
        if (outputFilename == null || outputFilename.isEmpty()) {
            throw new IllegalArgumentException("Output filename shouldn't be empty.");
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREADS_COUNT);

        try {
            Set<Long> result = processFile(inputFilename, keyWord, lineCount, executor);
            executor.shutdown();

            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }

            List<String> resultListString = doStringsTogether(inputFilename, result);
            writeInformationInFile(outputFilename, resultListString);

        } catch (Exception e) {
            throw new RuntimeException("Error processing file", e);
        }
    }
}