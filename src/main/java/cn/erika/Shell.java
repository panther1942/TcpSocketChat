package cn.erika;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shell {
    public static void main(String[] args) {

        List<String> cmd = new ArrayList<>();

        String regex = "(\"[^\".]+\"|\\S+)";
        Pattern p = Pattern.compile(regex);

        String target = "talk id0 \"this is a demo test\" aaa bbb ccc";
        Matcher m = p.matcher(target);

        while (m.find()) {
            cmd.add(m.group(1).replaceAll("\"", ""));
        }

        for (String param : cmd) {
            System.out.println(param);
        }
    }

}
