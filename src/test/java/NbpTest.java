import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NbpTest {
    @Test
    void testRoundTo() {
        //given
            double d = 24.453323;
        //when
        //TODO
        double result = Nbp.roundTo(d,4);
        //THEN
        Assertions.assertEquals(24.4533, result);
    }



}