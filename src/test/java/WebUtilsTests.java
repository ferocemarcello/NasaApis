import org.example.NasaException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.example.utils.WebUtils.sendGetRequest;
import static org.example.utils.WebUtils.sendGetRequestAndRead;
import static org.junit.jupiter.api.Assertions.*;

public class WebUtilsTests {

    @Test
    void testGetRequest(){
        try {
            HttpURLConnection httpURLConnection = sendGetRequest(new URL("https://api.publicapis.org/entries"), 0, 0);
            assertEquals(httpURLConnection.getResponseCode(),200);
            assertEquals(httpURLConnection.getResponseMessage(),"OK");
            HttpURLConnection failConnection = sendGetRequest(new URL("https://api.nasa.gov/neo/rest/v1/feed?end_date=2022-01-03&start_date=2022-01-01"), 0, 0);
            assertNotEquals(failConnection.getResponseCode(),200);
            assertNotEquals(failConnection.getResponseMessage(),"OK");
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    void testSendAndReceive(){
        try {
            String read = sendGetRequestAndRead(new URL("https://api.genderize.io/?name=John"), 0, 0);
            assertEquals(read,"{\"name\":\"John\",\"gender\":\"male\",\"probability\":0.99,\"count\":218952}");
        } catch (IOException | NasaException e) {
            fail();
        }
        try {
           sendGetRequestAndRead(new URL("https://api.nasa.gov/neo/rest/v1/feed?end_date=2022-01-03&start_date=2022-01-01"), 0, 0);
            fail();
        } catch (IOException e) {
            fail();
        } catch (NasaException e) {
            assertTrue(true);
        }
    }
}