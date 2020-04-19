package heap.language;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class FunctionsTest extends HeapLanguageTest {

    @Test
    public void classOf() {
        assertEquals("java.io.File", runJS("classof(heap.findObject(1684166976)).name").as(String.class));
    }

}
