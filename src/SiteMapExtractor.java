import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteMapExtractor extends RecursiveAction {

    private String pageUrl;
    private Set<String> urls;

    public SiteMapExtractor(Set<String> urls, String pageUrl) {
        this.pageUrl = pageUrl;
        this.urls = urls;
    }

    @Override
    protected void compute() {
        urls.add(pageUrl);                                                       // Добавляем строку-ссылку в коллекцию
        List<SiteMapExtractor> tasks = new ArrayList<>();                        // Создаём список задач для парсинга дочерних страниц

        for (String childUrl : getChildUrls()) {
            if (!urls.contains(childUrl)) {                                      // Проверяем наличие ссылки в коллекции
                SiteMapExtractor task = new SiteMapExtractor(urls, childUrl);    // В случае её отсутстви запускаем задачу для каждой дочерней ссылки
                task.fork();                                                     // Запускаем задачу асинхронно
                tasks.add(task);                                                 // Добавляем задачу в список дочерних страниц
            }
        }

        tasks.forEach(ForkJoinTask::join);                                       // запускаем задачи в пуле ForkJoinPool
    }

    /**
     * Парсит текущую страницу и возвращает ссылки на дочерние страницы
     */
    private Set<String> getChildUrls() {
        Set<String> urls = new HashSet<>();                                      // Создаём коллекцию URL адресов
        try {
            Document doc = Jsoup                                                 // Создаём переменную для загрузки строк
                    .connect(pageUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36")
                    .ignoreHttpErrors(true)
                    .get();
            URL baseUrl = new URL(pageUrl);
            urls = doc.select("a").stream()                             // Фильтруем полученные строки
                    .map(e -> getChildUrl(baseUrl, e.attr("href")))
                    .filter(u -> u.startsWith(pageUrl))
                    .collect(Collectors.toSet());


        } catch (Exception ex) {
            System.out.printf("Ошибка парсинга страницы '%s': %s%n", pageUrl, ex.getMessage());
        }

        return urls;                                                             // Возвращаем результат - ссылки на дочерние страницы
    }

    /**
     * Возвращает абсолютную ссылку на основе содержимого href,
     * отбрасывает часть ссылки после символа '#' (anchor)
     */
    private String getChildUrl(URL baseUrl, String href) {
        try {
            String childUrl = new URL(baseUrl, href).toString();
            int anchorIndex = childUrl.indexOf('#');
            if (anchorIndex > 0) {
                childUrl = childUrl.substring(0, anchorIndex);
            }
            return childUrl;
        } catch (MalformedURLException ex) {
            return "";
        }
    }

}
