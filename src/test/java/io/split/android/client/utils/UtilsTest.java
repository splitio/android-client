package io.split.android.client.utils;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UtilsTest extends TestCase {

    @Test
    public void test() throws Exception {
        List<String> strings = StringList();
        List<String> results = ResultList();

        for (int i = 0; i < strings.size(); i++) {
            String result = Utils.convertApiKeyToFolder(strings.get(i));
            Assert.assertEquals(results.get(i), result);
        }
    }

    private List<String> StringList() {
        return new ArrayList<>(Arrays.asList(
                "b1ca0fe5cee94f6599948a005c10fbf1",
                "b85c9a8d173c4f4196dd5bb38cf2c875",
                "9d07d43035294ad4b094ab675590ca61",
                "4f4e6871bb18482e84154c4762cbcbe9",
                "cdd570295a3a406b94410ba8e2b0e3b3",
                "d773376aa8cb4f1aa1bee1bf2939bb0f",
                "ed471ca50ab1411889904d5191c35aaf",
                "5223844b56e44f5daeb924c0b1478774",
                "77b71e4b81314d8aa53dbd7638d91f7c",
                "39db025ea68d45a7917f34a9c789dae9",
                "d2e15d3151c14588b3189da7e7b2796f",
                "c3c112e14d4949148b6f4e243984de5b",
                "247f68b47f1e4faea4a7754f7eac4261",
                "09df7bb3afeb4a779d8c5b873fad15c6",
                "7c1968fdd7044d069e9fd5752cd02a68",
                "2566866a5655410cab4ca2c953232c65",
                "c04f6ee08d4a4550b9d6ddf0f673214a",
                "ccfde9d46e814a268453eec412b6a0e3",
                "8a12e5f68b5b482ab360f2a7ca071ae5",
                "747471a8ef214c488d4ed02fdc4dae74",
                "7f702e165f8b42dba28fd77c31c19817",
                "67794791166946868befb6982dec0c8a",
                "4ee67cd578b44f939ddfd1880630f844",
                "e723a26d818042748b9a3864abc64660",
                "8a6ed8dcf261464e82fe125a97e3c3d2",
                "c91705d4558548c8bef6057a567101dc",
                "b520a56525a74c219c363dc680231994",
                "339702695b744e179c7d8832dd56ebf1",
                "e1fb46fa3f0046a2a5d6b6682e53d3f3",
                "57aaca7ac5d646d3945c20207b9c21c2",
                "0da787d4b4564f189af74f38b68cc9c2",
                "65f72324b55f4a32a4de279fde335e47",
                "091a8d1c0884451fa12d0294a24fafd6",
                "9db7ea5a40f44c9fa96d36711bebafa8",
                "490a575403f44a2e9de01b43f6eee3a6",
                "1383f893f4fa46ba875aa25e0f6095b1",
                "7e8a37eaef07421f85bcf0c6ce474044",
                "9ce1691780fc48079f7ab2160de3c802",
                "0574aa1bafde41468c063d9dda3e45d9",
                "013eba70e324440788310f0923fcd026"
        ));
    }

    private List<String> ResultList() {
        return new ArrayList<>(Arrays.asList(
                "4c90341b797ccf49b60fda2b0486e75f7357158f2a75958fa3",
                "532fedc115d25208071cad9df4c1b48285cc539a86f97eb5d2",
                "16153c8257dadb47f05bcb6e8af67d4167c4d66d637d5a4f49",
                "f95dcfb01588e6818b7832d92bc28b322ca0548e9f047b9196",
                "cab93529c7cf448955d795edbae70b3acccfc348681795f20b",
                "3ad8dcf3603bf699ea9463512886b1f2ac09e7a97e5330c9cf",
                "2458025314c041a1c12346bacc6f24e3e209daa10435647d4b",
                "2e30e497ec8adbfa75e48b6defaf28b8393d1cebab970c2407",
                "907158cac7cac2c6f6abe04a51984b540d8f0e150c043eeb2c",
                "456e63ed19f1bd154674f8aafacc8b5237293a37efd83af8a5",
                "91e1d5a42ae97a3157f90b63a51b1cb82d29e35428aa07d0ff",
                "3c18159d8d5c6b28659fe45d1adb6eef8be6717a7246d27d85",
                "e37a4d59006a7ba3f2b24d49a7b95456b848fb060bfa6dcd98",
                "4c98bd9ad076aa6d9ff083b2994981ede2622c6d53e980b93e",
                "b69427d1732fb2fabab8be16d73230e349adb4c68c487ff781",
                "b310fc6d2517702a68a979cf438c1fb4066167056b6eb3d65c",
                "6f9d08b5d204416290b8e185ddaed4639b60ade50c6488ac61",
                "4caf3a75184236f60684f8a2a6f5e50cdba8501867e60362bd",
                "8e9ff2f960a9f48439a83dc78e8aa472b55a807869631f0ad6",
                "9435a39d1ac6d08b06f6f5c35055971c557093ffcc7638117e",
                "e30a985f25c061d39ff54216163ddb3c8ec2c9d1065e499596",
                "5cc981830083ffc059fce301c15b8ffe87b5263aaeb8021671",
                "3f74ec3b670df7812b26cefc1f021d29178b29caf7ba424f62",
                "e523e68d289dd7a2598390c7bbc62eb1c1387c43333a5027e4",
                "542807029bd264a72cb185ecc793b2c9810e8a13071f880920",
                "89da6f4ed3f7689f6ddad6e2e9e7afa6d97b01f37a1af292f4",
                "0069fbd50fc472b0f7cc7ede5fff98fcbd652b92d60f335b5a",
                "37e009b9fce3b25ac6f2cfe96ba5370f31acc01e036f0cdf16",
                "8aaf3353b1e9292df5cb2eead1957c019c7664d2b5f1644b94",
                "f6748d550f6e07f7e851b56a0b54b6e2a0c4f1b1bd33fffc5c",
                "d355d2762e46b7434079716ecd9d121cf3e30e39188ad63847",
                "1d987d2464fc39842b1b841a0a2885d324f864ead5ddf4cf1f",
                "90e2db127e1aec58664974dfdd60a7206155cbfd018488993f",
                "278092b968e6c8849faad4a17cce2fd90b486ef4fcda9114a3",
                "696b14d81f2b89be050aae7399e27bbfb396c5f8773c393683",
                "732e4348ff6044ee7d33d6878db6612e9d231c397e856aa302",
                "d1c4a9542b30d5e4e89241655018be71b1345b0d864539958e",
                "b1e6a41edfe3a983a11187099d9d842ca55ce1017a8fa19c4e",
                "332273ef43a2dc3898e310d1e481664da828cde45535b28364",
                "7e9ec47a326692d6fe27e8b1cb0a0beab20e1a17a7351bd7b9"
        ));
    }
}
