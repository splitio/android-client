package io.split.android;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import io.split.android.client.utils.Utils;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class FolderForApiKeyTest {


    @Test
    public void testNormalApiKeys() throws Exception {

        ArrayList<String> apiKeys = new ArrayList<>(Arrays.asList(
                "2cca3f6c401911e9b210d663bdgabd873d93",
                "2cca4228401911e9b210d663bd873d93dgab",
                "2cca448a401911e9b210d663bdgabd873d93",
                "2cca45e84019dgab11e9b210d663bd873d93",
                "2cca4782401911e9bdgab210d663bd873d93",
                "2cdgabca4bba401911e9b210d663bd873d93",
                "2cca4d0dgab4401911e9b210d663bd873d93",
                "2cca4e3adgab401911e9b210d663bd873d93",
                "2cca4f704dgab01911e9b210d663bd873d93",
                "2cca509c401911e9b21dgab0d663bd873d93"
        ));

        ArrayList<String> folders = new ArrayList<>(Arrays.asList(
                "2a102cca3f6c401911e9b210duFc00jW4OiuO1H0qmNMaun8cWPKQC",
                "2a102cca4228401911e9b210duYXP9Q1Xupsi5PoGQsvPBp3skHeaa62",
                "2a102cca448a401911e9b210dupcUi1Cff5RyBC1eqMky1jrSFRXIHy",
                "2a102cca45e84019dgab11e9butnDebTY8PxgwLML6w8N8PxVFGTPQDw2",
                "2a102cca4782401911e9bdgabuRRxoNbhMIukmyefN8va8ict2hukvy",
                "2a102cdgabca4bba401911e9bunD5S2JchZ95RdIArdXrKYZcPJbovtD2",
                "2a102cca4d0dgab4401911e9buoFFnxAkZ7zIbL0q19I7YfcYupDmPJy",
                "2a102cca4e3adgab401911e9buTkoAXoZKWGUcgPbAEeS9hxhuSW4XGP",
                "2a102cca4f704dgab01911e9buq4q8wgkWkK1NIur7ux0aUnZsgW5BWK",
                "2a102cca509c401911e9b21dgOASEqrNDaDDzUeqvOvWxOa0q7BiO6ZPi"
        ));


        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String folder = Utils.convertApiKeyToFolder(apiKey);
            String expectedFolder = folders.get(i);
            Assert.assertEquals(expectedFolder, folder);
        }
    }

    @Test
    public void testShortApiKeys() throws Exception {

        ArrayList<String> apiKeys = new ArrayList<>(Arrays.asList(
                "61673d6242ee",
                "4ccdf432f526",
                "5233198c6eab",
                "75d5cd1bd2e7",
                "50e9e8bf5b13",
                "be3998817e7f",
                "0dce54a342d7",
                "adb0f1807a21",
                "3646df2f1972",
                "bf49fc403414"
        ));

        ArrayList<String> folders = new ArrayList<>(Arrays.asList(
                "2a1061673d6242eeAAAAAAAAAkVfmt8pO7kXng2dkTNBCvwAoVVxmgi",
                "2a104ccdf432f526AAAAAAAAAU8eDjEw7xc7J47i9usLnmugxdKCB3lO",
                "2a105233198c6eabAAAAAAAAASNi7QUwwfmBhZM33VGkKkLub3W0KhSC",
                "2a1075d5cd1bd2e7AAAAAAAAAqS8zHVO5wqOdi8XZzr55NpbTa22xC",
                "2a1050e9e8bf5b13AAAAAAAAA1TWXQarV2k1UiJFZSfr6NdMotIt5u6",
                "2a10be3998817e7fAAAAAAAAAPg2FEAA4On9KmK9oWd2c4oHEes8b1q",
                "2a100dce54a342d7AAAAAAAAAQfxU88HUZqvBvAwmrr2U8BDeSTUGw2",
                "2a10adb0f1807a21AAAAAAAAAfOs2MpMMhr3LgxSi35CuVqSCOEaC3Qq",
                "2a103646df2f1972AAAAAAAAAbEh7eEFpBwCSTYaC6YdMRlGRKOStq",
                "2a10bf49fc403414AAAAAAAAAJYLzu0pJkDCBG3qbAQVpuBxa9DzCHi"
        ));

        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String folder = Utils.convertApiKeyToFolder(apiKey);
            String expectedFolder = folders.get(i);
            Assert.assertEquals(expectedFolder, folder);
            folders.add(folder);
        }
    }

    @Test
    public void testDirtyApiKeys() throws Exception {

        ArrayList<String> apiKeys = new ArrayList<>(Arrays.asList(
                "2cca3c401911e9b21¡d66?3bdgabd873d93",
                "2cca4228(01911e9b210d66_bd873d93dga",
                "2cca448a401**11e9b210d663bd/abd873d",
                "2cca45\\84019dgab11e....0d663bd873d9",
                "2cca478\'40191\"e9bdgab210d663bd873da",
                "2cdgabca4bba401911e9b210d663()_+bd8",
                "2cca4d0dgab4401911e9b21{}[]0d663bd87",
                "2cca4e3adgab!@#$%^&*()_911e9b210d663",
                "2cca4f704dgab01911e:\"<>.,?d663bd873d",
                "-_2cca509c40...1911e9b0d663bd873d93+"
        ));

        ArrayList<String> folders = new ArrayList<>(Arrays.asList(
                "2a102cca3c401911e9b21d663OdzD3vRbju0cBLU8lIpp8XCM4PVWgGS",
                "2a102cca422801911e9b210d6ue3cZTcHdqEy4Lsx2bVVZheRpdML",
                "2a102cca448a40111e9b210d6uym1B9AlLAEyasP37OJkj82rygS2",
                "2a102cca4584019dgab11e0d6uMN1wrXL01BfB72YNmGnhNUdOjCe4C",
                "2a102cca47840191e9bdgab21uEdoQz8CPkdOZYvhNFZUl2TYc1ZbIRW",
                "2a102cdgabca4bba401911e9buIVshAq0uf6VBBUA2pkjvEI8a1KfBi",
                "2a102cca4d0dgab4401911e9buXJwyLfAFzgTTAiKbmbpVuZ4qswAaFQO",
                "2a102cca4e3adgab911e9b210Of3cCgzM2E7OuqPgNP1UkD2ntQmFoqJG",
                "2a102cca4f704dgab01911ed6uCXIzERJKOwQoJCIzB0Wxz1tUYjK",
                "2a102cca509c401911e9b0d66u9tftZfrQI4mnJtMro60srnrcQ3tuHy"
        ));


        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String folder = Utils.convertApiKeyToFolder(apiKey);
            String expectedFolder = folders.get(i);
            Assert.assertEquals(expectedFolder, folder);
        }
    }

    @Test
    public void testLongApiKeys() throws Exception {

        ArrayList<String> apiKeys = new ArrayList<>(Arrays.asList(
                "2a102cca3f6c401911e9b210duFc00jW4OiuO1H0qmNMaun8cWPKQC",
                "2a102cca4228401911e9b210duYXP9Q1Xupsi5PoGQsvPBp3skHeaa62",
                "2a102cca448a401911e9b210dupcUi1Cff5RyBC1eqMky1jrSFRXIHy",
                "2a102cca45e84019dgab11e9butnDebTY8PxgwLML6w8N8PxVFGTPQDw2",
                "2a102cca4782401911e9bdgabuRRxoNbhMIukmyefN8va8ict2hukvy",
                "2a102cdgabca4bba401911e9bunD5S2JchZ95RdIArdXrKYZcPJbovtD2",
                "2a102cca4d0dgab4401911e9buoFFnxAkZ7zIbL0q19I7YfcYupDmPJy",
                "2a102cca4e3adgab401911e9buTkoAXoZKWGUcgPbAEeS9hxhuSW4XGP",
                "2a102cca4f704dgab01911e9buq4q8wgkWkK1NIur7ux0aUnZsgW5BWK",
                "2a102cca509c401911e9b21dgOASEqrNDaDDzUeqvOvWxOa0q7BiO6ZPi"
        ));

        ArrayList<String> folders = new ArrayList<>(Arrays.asList(
                "2a102a102cca3f6c401911e9buUvEc3uRECQrmlQXEJbR6rUX60TotoKS",
                "2a102a102cca4228401911e9buJOzlEYywqfBxqrDhQ1CvoBbK4up2",
                "2a102a102cca448a401911e9bu4uquIInz128RzSb7mi66Io7X3G59O2",
                "2a102a102cca45e84019dgab1uXnYmhJMFxCcsyScZJ6IcRfi3NSWoGae",
                "2a102a102cca4782401911e9bO8R4SpzayZSEX5K7G1NRwDoq1kNh2L",
                "2a102a102cdgabca4bba40191u0QXhs0fsBEvvtWBRIMMAGs5V4Oh2W",
                "2a102a102cca4d0dgab440191uI0h6qTAM9ZUjuDsSRIqightHvrlz5K",
                "2a102a102cca4e3adgab40191uGJgEJhfdL2hfTwKXJDSMF5f3biQZOC",
                "2a102a102cca4f704dgab0191uXAfLROKEUWLHSQBhIHDcQfNBiYFiKq",
                "2a102a102cca509c401911e9buvaxSf1FWzEAcfdExgmdUjmpA3tHe"
        ));

        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String folder = Utils.convertApiKeyToFolder(apiKey);
            String expectedFolder = folders.get(i);
            Assert.assertEquals(expectedFolder, folder);
        }
    }
}