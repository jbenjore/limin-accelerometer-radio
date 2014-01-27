package limn.radio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.io.Files;
import com.google.common.io.LineReader;

public class LineReadingIterator extends AbstractIterator<String> {
    private final LineReader lineReader;
    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    public LineReadingIterator(String fileName) {
        checkNotNull(fileName, "fileName");
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = Files.newReader(file, DEFAULT_CHARSET);
        } catch (FileNotFoundException e) {
            Throwables.propagate(e);
        }
        this.lineReader = new LineReader(reader);
    }

    @Override
    protected String computeNext() {
        String line = null;
        try {
            line = this.lineReader.readLine();
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return line == null ? endOfData() : line;
    }
}
