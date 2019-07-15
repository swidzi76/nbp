import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
// -------------------------------- DO POPRAWY -------------------------------------------
//----------------------------------------------------------------------------------------
//
// OK ----------> 1) Brakuje `pom.xml`i opcjonalnie `.gitignore`. I przez 1 commita trochę widać, że nie pracowałeś na gicie, tylko wrzuciłeś to "bo taki był wymóg" :wink:
//        2) Nie ma żadnych testów
// OK ----------> 3) Klasy dla GSONa: Jest dobrze, czysto i klarownie - za to plus! Tak dla Twojej wiadomości,
//        można to było też rozwiązać inaczej - wsadzając w klasę `Rate` zarówno `bid` i `ask` jak teraz, a ponadto `mid`.
//        Wówczas moglibyśmy używać tej samej klasy do obu tabel
//        (ale podział byłby brzydszy i musielibyśmy panować nad tym, co wczytujemy).
//        Twoje rozwiązanie jest lepsze, bardziej czyste - ale wymaga więcej kodu.
//        Pisze po prostu żebyś miał świadomość, że da się inaczej :wink:
// OK ----------->4) Brzydki komentarz w `AverageRate` . Czepiam się, wiem :wink:
// OK ----------->5) Ładny pomysł z `List<String> currency`!
//          Ewentualnie moglibyśmy to tez rozwiązać przez enuma z tymi wartościami - byłby trochę bardziej czysto,
//          ale tak już i tak jest super. Z małyms szczegółem patrz (7)
// OK ----------->6) `Nbp` linia `38`: `ExchangeRate exchangeRate = new ExchangeRate();`.
//        A dwie linijki później wołasz `exchangeRate = getExchangeRate(s);`.
//        W takiej sytuacji nie ma zadnego sensu w tworzeniu `new ExchangeRate()`
//        przed pętla - ta wartośc i tak nie ma żadnego znaczenia.
//        Możesz zostawić ją pustą (deklaracja `ExchangeRate exchangeRate` bez przypisania wartosci) lub ustawić na `null`.
// OK ----------->7) `for (String s : currency) {` zamiast `s` przydałaby się wartościowa nazwa`. Np. `currency`, a lista nazwana `currencies`
// OK ----------->8) Zamiast `(rate == TypeOfRate.BUYING) ?`
//          byłoby bardziej czytelnie zastosować pełnego ifa albo `switch`a.
//          Tehcnicznie jest poprawnie, ale trudno się czyta.
//          A switch znacznie by nam to poprawił (pod wzgledem czytelnosci)
// OK ----------> 9) `getExchangeRateForDate` i `getAverageExchangeRate` są bardzo podobne. Może dałoby się je jakoś sprowadzić do jednej metody?
// OK ----------> 10) Połowa `getCorrectDate` bardzo ładnie, a druga połowa strasznie na piechotę! Użyj tego z formatem `yyyy-MM-dd`: http://tutorials.jenkov.com/java-internationalization/simpledateformat.html
//11) `private static double roundTo(double value, int digits){` - formatowanie liczby możemy zrobić w ładnym stylu - tak, jak jest opisane w internecie: https://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
//12) Ogółem podoba mi się Twoje rozbijanie rzeczy na metody - fajny masz styl :slightly_smiling_face:
//----------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------
public class Nbp {
    public static void main(String[] args) throws IOException {
        System.out.println("Program pobiera aktualne kursy dla USD, EUR, GBP, CHF");
        List<String> currencies = new ArrayList<>();
        currencies.add("USD");
        currencies.add("EUR");
        currencies.add("GBP");
        currencies.add("CHF");

        double plnForExchange = 100;

        System.out.println("-------------------------------------------------------------");
        System.out.println("KURSY ŚREDNIE");
        for (String currency : currencies) {
            System.out.println(currency + " : " + getAverageExchangeRate(currency).rates[0].mid);
        }
        System.out.println(plnForExchange + " PLN to odpowiednio :");
        for (String currency : currencies) {
            System.out.println(currency + " : " + averageExchangePlnFor(currency,plnForExchange));
        }

        //---------------------------------------------------------------------------------
        // ZADANIA DODATKOWE
        System.out.println("-------------------------------------------------------------");
        System.out.println(" KURSY SPRZEDAŻY I KUPNA  - AKTUALNE");
        ExchangeRate exchangeRate = new ExchangeRate();
        for (String currency : currencies) {
            exchangeRate = getActualExchangeRate(currency);
            System.out.println(currency + " : " + " KUPNO : " + exchangeRate.rates[0].bid
                    + " SPRZEDAŻ : " + exchangeRate.rates[0].ask);
        }
        // za 100 PLN możemy kupić po kursie sprzedaży:
        System.out.println("-------------------------------------------------------------");
        System.out.println(plnForExchange + " PLN to odpowiednio po kursie SPRZEDAŻY:");
        for (String currency : currencies) {
            System.out.println(currency + " : " + exchangePlnFor(currency,"",plnForExchange,TypeOfRate.SELLING));
        }

        LocalDate localDate = LocalDate.parse(exchangeRate.rates[0].effectiveDate);
        String dateMonthAgo = getCorrectDate(localDate.minusMonths(1));
        System.out.println("-------------------------------------------------------------");
        System.out.println(" KURSY SPRZEDAŻY I KUPNA  - MIESIĄC TEMU - "+ dateMonthAgo);
        for (String currency : currencies) {
            exchangeRate = getExchangeRateForDate(currency,dateMonthAgo);
            System.out.println(currency + " : " + " KUPNO : " + exchangeRate.rates[0].bid
                    + " SPRZEDAŻ : " + exchangeRate.rates[0].ask);
        }

        // kupujemy miesiąc temu za 100 PLN i sprzedajemy teraz
        System.out.println("-------------------------------------------------------------");
        System.out.println(" 100 PLN zamienione na walutę 1 miesiąc temu (sprzedane) ");
        System.out.println(" aktualnie walutę wymieniamy na PLN ");
        System.out.println(" TERAZ - MIESIĄC TEMU");
        for (String currency : currencies) {
            double currencyPast = exchangePlnFor(currency,dateMonthAgo,plnForExchange,TypeOfRate.SELLING);
            double currencyNow  = exchangeToPln(currency,"", currencyPast, TypeOfRate.BUYING);
            System.out.println(currency + " : " + roundTo(currencyNow - plnForExchange, 4));
        }

    }
    public static double exchangePlnFor(String currency, String dateRate, double valuePln, TypeOfRate rate) throws IOException {
        ExchangeRate exchangeRate = getExchangeRateForDate(currency, dateRate);
        if(rate == TypeOfRate.BUYING){
            return roundTo((valuePln / exchangeRate.rates[0].bid),4);
        }else{
            return roundTo((valuePln / exchangeRate.rates[0].ask),4);
        }
    }
    public static double exchangeToPln(String currency, String dateRate, double valueCurrency, TypeOfRate rate) throws IOException {
        ExchangeRate exchangeRate = getExchangeRateForDate(currency, dateRate);
        double temp = (rate == TypeOfRate.BUYING) ? valueCurrency * exchangeRate.rates[0].bid :
                valueCurrency * exchangeRate.rates[0].ask;
        return roundTo(temp, 4);

    }
    public static double averageExchangePlnFor(String currency, double valuePln) throws IOException {
        double temp = valuePln / getAverageExchangeRate(currency).rates[0].mid;
        return roundTo(temp, 4);
    }
    public static ExchangeRate getActualExchangeRate(String currency) throws IOException {
        return getExchangeRateForDate(currency, "");
    }
    ///////

    public static ExchangeRate getExchangeRateForDate(String currency, String date) throws IOException {
        StringBuilder sb = getJsonFromNbp(currency, "c",date);
        Gson gson = new Gson();
        ExchangeRate exchangeRate = gson.fromJson(sb.toString(), ExchangeRate.class);
        return exchangeRate;
    }
    //////
    public static ExchangeAverageRate getAverageExchangeRate(String currency) throws IOException {
        StringBuilder sb = getJsonFromNbp(currency, "a", "");
        Gson gson = new Gson();
        ExchangeAverageRate exchangeAverageRate= gson.fromJson(sb.toString(), ExchangeAverageRate.class);
        return exchangeAverageRate;
    }

    private static StringBuilder getJsonFromNbp(String currency, String table, String date) throws IOException {
        final URL url = new URL("http://api.nbp.pl/api/exchangerates/rates/"+
                table.toLowerCase()+"/"+currency.toLowerCase()+"/"+date+"/");
        final URLConnection urlConnection = url.openConnection();
        urlConnection.addRequestProperty("User-Agent", "Chrome");
        final BufferedReader in = new BufferedReader(new InputStreamReader(
                urlConnection.getInputStream()));
        String inputLine;
        StringBuilder sb = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        in.close();
        return sb;
    }
    private static String getCorrectDate(LocalDate date){
        //  if date falls in SUNDAY , we back two days - to friday
        if(date.getDayOfWeek() == DayOfWeek.SUNDAY){
            date = date.minusDays(2);
        }
        // if date falls in SATURDAY, we back one day - to friday
        if(date.getDayOfWeek() == DayOfWeek.SATURDAY){
            date = date.minusDays(1);
        }

        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(Date.valueOf(date));
    }
    public static double roundTo(double value, int digits){
        BigDecimal bd = new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}
