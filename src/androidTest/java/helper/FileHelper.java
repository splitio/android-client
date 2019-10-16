package helper;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileHelper {

    public String loadFileContent(Context c, String name) {

        String content = null;
        try {
            InputStream fin = c.getAssets().open(name);
            content = convertStreamToString(fin);
            fin.close();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return content;
    }

    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}
