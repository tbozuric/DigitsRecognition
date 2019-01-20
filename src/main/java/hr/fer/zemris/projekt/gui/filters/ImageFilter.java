package hr.fer.zemris.projekt.gui.filters;

import java.io.File;
import java.io.FilenameFilter;

public class ImageFilter implements FilenameFilter {
    private static final String[] EXTENSIONS;
    static {
        EXTENSIONS = new String[]{"png", "jpg", "jpeg"};

    }

    @Override
    public boolean accept(File dir, String name) {
        for (String extension : EXTENSIONS) {
            if (name.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }
}
