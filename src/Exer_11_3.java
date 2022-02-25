import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class Exer_11_3 {

    public static Set<String> uniqueURL = new HashSet<String>();

    public static void main(String[] args) throws IOException {
        final String SOURCE_URL = "https://skillbox.ru/";                // Creating of new variable with URL for downloading lines
        final String DEST_FILE = "out\\dest.txt";


        try {
            Document doc = Jsoup.connect(SOURCE_URL).maxBodySize(0)
                    .timeout(0)
                    .ignoreHttpErrors(true)
                    .get();

            Elements lineNames = doc.select("a");      // Getting lines

            lineNames.forEach(l -> {
                String attr = l.attr("abs:href");

                String[] str = attr.split("/");
                try {
                    if (str[2].equals("skillbox.ru") && str.length <= 4) System.out.println(attr);
                    System.out.println(attr);
                } catch (IndexOutOfBoundsException exc) {
                    System.out.println(exc.getMessage());
                }
            });
        } catch (Exception exp) {
            System.out.println(exp.getMessage());
        }

        Set<String> urls = Collections.synchronizedSet(new HashSet<>());                   // потокобезопасная коллекция для ссылок на дочерние страницы
        SiteMapExtractor rootTask = new SiteMapExtractor(urls, SOURCE_URL);                 // собираем переходы на дочерние страницы со всех страниц сайта, начиная с корневого
        new ForkJoinPool().invoke(rootTask);

        List<String> indentedUrls = urls.stream().sorted(Comparator.comparing(u -> u))        // упорядочиваем собранные ссылки, добавлем отступы и сохраняем в файл
                .map(u -> StringUtils.repeat('\t', StringUtils.countMatches(u, "/") - 2) + u)
                .collect(Collectors.toList());
        Files.write(Paths.get(DEST_FILE), indentedUrls);
    }
}
