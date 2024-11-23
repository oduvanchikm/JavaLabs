package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IDirectorySizeChecker;
import ru.mai.lessons.rpks.exception.DirectoryAccessException;

import java.io.File;

public class DirectorySizeChecker implements IDirectorySizeChecker {

    public static final String PREFIX = "src/test/resources/";

    @Override
    public String checkSize(String directoryName) throws DirectoryAccessException {
        if (directoryName == null || directoryName.isEmpty()) {
            throw new DirectoryAccessException("Directory name is null or empty");
        }

        File directory = new File(PREFIX + directoryName);

        if (!directory.exists()) {
            throw new DirectoryAccessException("Directory does not exist: " + directory.getPath());
        }

        if (!directory.isDirectory()) {
            throw new DirectoryAccessException("The provided path is not a directory: " + directory.getPath());
        }

        long totalResult;
        try {
            totalResult = calculateDirectorySize(directory);
        } catch (Exception e) {
            throw new DirectoryAccessException("Error while accessing directory: " + directory.getPath());
        }

        return formatSize(totalResult, directory.getPath());
    }

    private long calculateDirectorySize(File directory) throws DirectoryAccessException {
        if (directory == null) {
            return 0;
        }

        long result = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            throw new DirectoryAccessException("Unable to list files in directory: " + directory.getPath());
        }

        for (File file : files) {
            if (file.isDirectory()) {
                result += calculateDirectorySize(file);
            } else {
                result += file.length();
            }
        }

        return result;
    }

    private String formatSize(long size, String directoryName) {
        if (size < 0) {
            throw new IndexOutOfBoundsException("Size can't be les then 0");
        }

        String bytes = size + " bytes";
        System.out.printf("%s ---- %d bytes / %.2f KB / %.2f MB / %.2f GB%n",
                directoryName, size,
                size / 1024.0,
                size / (1024.0 * 1024),
                size / (1024.0 * 1024 * 1024));

        return bytes;
    }
}
