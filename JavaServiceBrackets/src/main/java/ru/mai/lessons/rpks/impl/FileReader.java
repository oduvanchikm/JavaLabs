package ru.mai.lessons.rpks.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.lessons.rpks.IFileReader;
import ru.mai.lessons.rpks.exception.FilenameShouldNotBeEmptyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileReader implements IFileReader {

    @Override
    public List<String> loadContent(String filePath) throws FilenameShouldNotBeEmptyException {
        if (filePath == null || filePath.isEmpty()) {
            throw new FilenameShouldNotBeEmptyException("error");
        }
        List<String> res = new ArrayList<>();
        File file = new File(filePath);
        try {
            Scanner input = new Scanner(file);
            while (input.hasNextLine()) {
                res.add(input.nextLine());
            }
        } catch (FileNotFoundException e) {
            return res;
        }

        return res;
    }
}