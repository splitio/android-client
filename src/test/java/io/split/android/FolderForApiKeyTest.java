package io.split.android;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import io.split.android.client.utils.Utils;

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
                "dbc697ed0063c3477942f0105eb798c152119ca3950c561a47",
                "7f402cd72b88dd4c175c839af7173f7906d2c2e950d0e14a78",
                "c3a1ef94f1ea1cbba4d9294904e11ba3473420fe26ed46b42d",
                "533d88cd96c48822c83753bc1f7a29a7cd4387289dd355224e",
                "8031ebef5a57542eb0e73a3fba4ba67f63b3bb915d4e64b693",
                "d69dacc03fd87d86888db2acbea0ff5d605cbb2f9bbc309051",
                "b8192f6dfed8444e5e80416f46bc6cdc48e1fdcb566e0d8322",
                "119e86f367d95cb2d03bbf3652edcebe961c466504d81c9566",
                "d2b6d955720d64253fda01d4efc72b439ca8547d4d9b9b8134",
                "3cbaa79e134b148fc919f04fb84f7e53929238149c2c3db28b"
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
                "79082582390ca6d34e113618b598df5c997913f78aae2ad4ee",
                "1abef10dbea0ca8e3be046cc58e39df834171e7a612581dbd2",
                "bc73db8316e12c5f84ad2a494ab50367395c0d036d0a6056dc",
                "15421e5dcb3f1c5471d140084e2895e3a2aaf103dc6b7c29ad",
                "82fedb4334bd5789103d69fa19b8f41c653c964a53a6092eb2",
                "d4a690dda44168ff4581da78958705d45b38a18bd8dbffb05c",
                "92245902d5417e0c9aee7a8e0c382192c62e6d19e5c55b200a",
                "6ac2e53aafb0d06b40f8846c7e43a3d13386e28f26fb390c6f",
                "c702df87e8785a2bf80105fd86c165a7598ff0ce7c1a4f9bb6",
                "bb66f53f6c391bc19fd284c454371c998f619bffccd9c281bc"
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
                "9d146253581d93c7d51639a5f75181dd05621ce5eff198666c",
                "6c62c5a836889ab43a5ebbbbb530ef589b114893f8bf614626",
                "12fdbd9d137ab70897bd6241173dfe351ea8b2b42c3a05ade9",
                "4cf83afdc141ba9c177c9e8efa7e7243165177bfc5ab4ec8b2",
                "9a70d6bac59f811d33ec90fefd401cba01eacf22cf299e0f1b",
                "389bb473f50140f67b9ad0b911f6d50fbef04059b2defedbee",
                "183ba6b86f4049a0191c1334046c8611a11f45b69e30e365a9",
                "b2fa6023ad989757c26c27c3a6452a2529b1b95a3ed097e9f4",
                "9acef590c4f1daf5e472beca6ebabb9d90360cc6cd0d5d0d23",
                "ee3424b69affd01de49e1e2970e5ef4b5acedb8b68e4dac0e4"
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
                "d50d9012cb1b8201e0781b8cff482945455ab7f38aea9fe43f",
                "f569f09c1d0b53a0b7dcf2a207c7e5bb86a4a16fade694da60",
                "d2ca015b378f4362f3ce30bd3f3768fe510507f5b8e766c3aa",
                "16b218105d2f321aa272364389b2230026e815931f7bab5af1",
                "a197550d835b2b6e533032cfaaa17d6747c017575d28afc187",
                "8b4e0a749119b714b5e50628aacbf10f573ddfdc7a4a09f460",
                "29d940aa4b13834fb6449b89bfc0c02766ea091834441729a2",
                "5680c54d25df7f4dc631864f9a82dfc918d51b0d0da8664227",
                "f54bcb2240cc731852a8b9c3ee483305609c4e1292ca5f777c",
                "7fefc2ad7b08898a501f345269078273bb17d346179b74c0a2"
        ));

        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String folder = Utils.convertApiKeyToFolder(apiKey);
            String expectedFolder = folders.get(i);
            Assert.assertEquals(expectedFolder, folder);
        }
    }
}
