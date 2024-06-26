package de.verdox.mccreativelab.util.io;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class StringAlign {
    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";

    @Deprecated
    public static List<String> justifyText (String text, int length) {
        int end = length, extraSpacesPerWord = 0, spillOverSpace = 0;
        String[] words;

        var builder = new StringBuilder();
        var list = new LinkedList<String>();

        while (end < text.length()) {

            end = text.lastIndexOf(" ", length);
            words = text.substring(0, end).split(" ");
            extraSpacesPerWord = (length - end) / words.length;
            spillOverSpace = length - end + (extraSpacesPerWord * words.length);

            for (String word : words) {
                builder.append(word).append(" ");
                builder.append((extraSpacesPerWord-- > 0) ? " " : "");
                builder.append((spillOverSpace-- > 0) ? " " : "");
            }
            list.add(builder.toString());
            builder = new StringBuilder();
            text = text.substring(end + 1);

        }
        return list;
    }

    @Deprecated
    public static List<String> createLines(String input, int maxCharsPerLine) {
        var result = new LinkedList<String>();

        var charCounter = 0;
        var words = input.split(" ");
        var lineBuilder = new StringBuilder();
        var forceBreakLine = false;
        for (String word : words) {
            if (word.length() + charCounter > maxCharsPerLine || forceBreakLine) {
                result.add(lineBuilder.toString());
                lineBuilder = new StringBuilder();
                charCounter = 0;
                forceBreakLine = false;
            }
            if (word.contains("\n")) {
                forceBreakLine = true;
                charCounter -= "\n".length();
            }
            lineBuilder.append(word.replace("\n", "")).append(" ");
            charCounter += word.length();
        }
        result.add(lineBuilder.toString());
        return result;
    }

    public static List<String> formatStringToLines(String input, int maxChars, Alignment alignment) {
        var output = new LinkedList<String>();
        var words = input.split(" ");
        var charCounter = 0;
        var builder = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            var word = words[i];
            var spaceNeeded = charCounter == 0 ? 0 : 1; // Kein Leerzeichen für das erste Wort der Zeile

            // Entscheiden, ob ein neues Wort + evtl. Leerzeichen passt oder nicht
            if (charCounter + spaceNeeded + word.length() > maxChars) {
                output.add(alignment.align(builder.toString(), maxChars));
                builder = new StringBuilder();
                charCounter = 0;
                spaceNeeded = 0; // Zurücksetzen, da es das erste Wort der neuen Zeile ist
            }

            // Leerzeichen hinzufügen, wenn es nicht das erste Wort in der Zeile ist
            if (charCounter > 0) {
                builder.append(" ");
                charCounter++; // Leerzeichen zählen
            }

            // Wort hinzufügen
            builder.append(word);
            charCounter += word.length();
        }

        // Den Inhalt des Builders zur Liste hinzufügen, wenn noch etwas vorhanden ist
        var content = builder.toString();
        if (!content.isEmpty())
            output.add(alignment.align(content, maxChars));

        return output;
    }

    @Deprecated
    public static List<String> format(String input, int maxChars, Alignment alignment) {
        List<String> strings = splitInputString(input, maxChars);

        var where = new StringBuffer();
        where = new StringBuffer();

        System.out.println(strings);

        for (String wanted : strings) {

            //Get the spaces in the right place.
            switch (alignment) {
                case RIGHT -> {
                    pad(where, maxChars - wanted.length());
                    where.append(wanted);
                }
                case CENTER -> {
                    int toAdd = maxChars - wanted.length();
                    pad(where, toAdd / 2);
                    where.append(wanted);
                    pad(where, toAdd - toAdd / 2);
                }
                case LEFT -> {
                    where.append(wanted);
                    pad(where, maxChars - wanted.length());
                }
            }
            //list.add(where.toString());
        }

        return Arrays.stream(where.toString().split("\n")).toList();
    }

    protected static void pad(StringBuffer to, int howMany) {
        to.append(" ".repeat(Math.max(0, howMany)));
    }

    private static List<String> splitInputString(String str, int maxChars) {
        List<String> list = new LinkedList<>();
        if (str == null)
            return list;
        for (int i = 0; i < str.length(); i = i + maxChars) {
            int endindex = Math.min(i + maxChars, str.length());
            list.add(str.substring(i, endindex));
        }
        return list;
    }

    public enum Alignment {
        LEFT {
            @Override
            public String align(String text, int maxCharsPerLine) {
                return StringUtils.rightPad(text, maxCharsPerLine);
            }
        },
        CENTER {
            @Override
            public String align(String text, int maxCharsPerLine) {
                if (maxCharsPerLine > text.length())
                    return StringUtils.center(text, maxCharsPerLine);
                else
                    return text.concat(" ".repeat(text.length() / 2));
            }
        }, RIGHT {
            @Override
            public String align(String text, int maxCharsPerLine) {
                if (maxCharsPerLine > text.length())
                    return StringUtils.leftPad(text, maxCharsPerLine);
                else
                    return text.concat(" ".repeat(text.length()));
            }
        },
        ;

        Alignment() {
        }

        public abstract String align(String text, int maxCharsPerLine);
    }
}
