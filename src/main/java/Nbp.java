import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Nbp {
    public static void main(String[] args) throws IOException {
        System.out.println("Program pobiera aktualne kursy dla USD, EUR, GBP, CHF");
        List<String> currency = new ArrayList<>();
        currency.add("USD");
        currency.add("EUR");
        currency.add("GBP");
        currency.add("CHF");

        double plnForExchange = 100;

        System.out.println("-------------------------------------------------------------");
        System.out.println("KURSY ŚREDNIE");
        for (String s : currency) {
            System.out.println(s + " : " + getAverageExchangeRate(s).rates[0].mid);
        }
        System.out.println(plnForExchange + " PLN to odpowiednio :");
        for (String s : currency) {
            System.out.println(s + " : " + averageExchangePlnFor(s,plnForExchange));
        }

        //---------------------------------------------------------------------------------
        // ZADANIA DODATKOWE
        System.out.println("-------------------------------------------------------------");
        System.out.println(" KURSY SPRZEDAŻY I KUPNA  - AKTUALNE");
        ExchangeRate exchangeRate = new ExchangeRate();
        for (String s : currency) {
            exchangeRate = getExchangeRate(s);
            System.out.println(s + " : " + " KUPNO : " + exchangeRate.rates[0].bid
                    + " SPRZEDAŻ : " + exchangeRate.rates[0].ask);
        }
        // za 100 PLN możemy kupić po kursie sprzedaży:
        System.out.println("-------------------------------------------------------------");
        System.out.println(plnForExchange + " PLN to odpowiednio po kursie SPRZEDAŻY:");
        for (String s : currency) {
            System.out.println(s + " : " + exchangePlnFor(s,"",plnForExchange,TypeOfRate.SELLING));
        }

        LocalDate localDate = LocalDate.parse(exchangeRate.rates[0].effectiveDate);
        String dateMonthAgo = getCorrectDate(localDate.minusMonths(1));
        System.out.println("-------------------------------------------------------------");
        System.out.println(" KURSY SPRZEDAŻY I KUPNA  - MIESIĄC TEMU - "+ dateMonthAgo);
        for (String s : currency) {
            exchangeRate = getExchangeRateForDate(s,dateMonthAgo);
            System.out.println(s + " : " + " KUPNO : " + exchangeRate.rates[0].bid
                    + " SPRZEDAŻ : " + exchangeRate.rates[0].ask);
        }

        // kupujemy miesiąc temu za 100 PLN i sprzedajemy teraz
        System.out.println("-------------------------------------------------------------");
        System.out.println(" 100 PLN zamienione na walutę 1 miesiąc temu (sprzedane) ");
        System.out.println(" aktualnie walutę wymieniamy na PLN ");
        System.out.println(" TERAZ - MIESIĄC TEMU");
        for (String s : currency) {
            double currencyPast = exchangePlnFor(s,dateMonthAgo,plnForExchange,TypeOfRate.SELLING);
            double currencyNow  = exchangeToPln(s,"", currencyPast, TypeOfRate.BUYING);
            System.out.println(s + " : " + roundTo(currencyNow - plnForExchange, 4));
        }

    }
    public static double exchangePlnFor(String currency, String dateRate, double valuePln, TypeOfRate rate) throws IOException {
        ExchangeRate exchangeRate = getExchangeRateForDate(currency, dateRate);
        double temp = (rate == TypeOfRate.BUYING) ? valuePln / exchangeRate.rates[0].bid :
                valuePln / exchangeRate.rates[0].ask;
        return roundTo(temp,4);
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
    public static ExchangeRate getExchangeRateForDate(String currency, String date) throws IOException {
        StringBuilder sb = getJsonFromNbp(currency, "c",date);
        Gson gson = new Gson();
        ExchangeRate exchangeRate = gson.fromJson(sb.toString(), ExchangeRate.class);
        return exchangeRate;
    }
    public static ExchangeRate getExchangeRate(String currency) throws IOException {
        return getExchangeRateForDate(currency, "");
    }
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
        // in sent string month and day must by two char length - we add "0" when < 10
        String  year = "" + date.getYear();
        String month = (date.getMonthValue() < 10) ? "0"+date.getMonthValue() : ""+date.getMonthValue();
        String day = (date.getDayOfMonth() < 10) ? "0"+date.getDayOfMonth() : ""+date.getDayOfMonth();
        return year+"-"+month+"-"+day;
    }
    private static double roundTo(double value, int digits){
        return Math.round(value * Math.pow(10,digits)) / Math.pow(10,digits);
    }
}
