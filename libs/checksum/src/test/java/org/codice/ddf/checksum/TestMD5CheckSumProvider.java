package org.codice.ddf.checksum;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import org.codice.ddf.checksum.impl.MD5CheckSumProvider;
import org.junit.Before;
import org.junit.Test;

public class TestMD5CheckSumProvider {


    private CheckSumProvider checkSumProvider;

    @Before
    public  void  intialize() {
        //TODO: Need to mock logger or something
        checkSumProvider = new MD5CheckSumProvider();
    }

    @Test
    public  void  testCalculateCheckSumString() {

        String testString = "Hello World";
        String checkSumCompareHash = "324D54D92B2D97471F9F4624596EA9F5";

        InputStream stringInputStream = getInputStreamFromObject(testString);
        String checkSumValue = checkSumProvider.calculateCheckSum(stringInputStream);

        //compare returned checksum to previous checksum
        //as they should be the same if the checksum is calculated
        //correctly
        assert(checkSumValue.equals(checkSumCompareHash));
    }

    @Test
    public  void  testCalculateCheckSumObject() {


        SerializableTestObject obj = new SerializableTestObject();
        obj.setName("Test Name");
        obj.setDescription("Test Description");

        String checkSumCompareHash = "010A1A1EFA1ACD8F9FBE78E80015B03C";

        InputStream stringInputStream = getInputStreamFromObject(obj);
        String checkSumValue = checkSumProvider.calculateCheckSum(stringInputStream);

        //compare returned checksum to previous checksum
        //as they should be the same if the checksum is calculated
        //correctly
        assert(checkSumValue.equals(checkSumCompareHash));
    }

    @Test
    public  void  testCalculateCheckSumWithNullInputStream() {

        String checkSumValue = checkSumProvider.calculateCheckSum(null);

        //compare returned checksum to previous checksum
        //as they should be the same if the checksum is calculated
        //correctly
        assert(checkSumValue == null);
    }

    @Test
    public  void  testGetCheckSumAlgorithm() {

        String expectedAlgorithm = "MD5";
        String checkSumAlgorithm = checkSumProvider.getCheckSumAlgorithm();

        //it is a problem if we compare these and they
        //are not the same because it could break alot
        assert(expectedAlgorithm.equals(checkSumAlgorithm));
    }

    private InputStream getInputStreamFromObject(Object obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objOutputStream;
        try {
            objOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objOutputStream.writeObject(obj);
            objOutputStream.flush();
            objOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return  inputStream;
    }
}
