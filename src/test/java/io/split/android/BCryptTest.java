package io.split.android;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.utils.BCrypt;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BCryptTest {


    @Test
    public void test() throws Exception {

        List<String> strings = StringList();
        List<String> results = ResultList();
        List<String> salts = SaltList();

        int resultIndex = 0;
        for(int j=0; j<5; j++){
            String salt = salts.get(j);
            for (int i = 0; i < strings.size(); i++) {
                String string = strings.get(i);
                String result = BCrypt.hashpw(string, salt);
                String expectedResult = results.get(resultIndex);
                Assert.assertEquals(expectedResult, result);
                resultIndex++;
            }
        }
    }

    List<String> StringList() {
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

    List<String> ResultList(){
        return new ArrayList<>(Arrays.asList(
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJu/ahGvcmhmkbMHaHBXQHXLhTU.Tp0.Mu",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuxZXmNeqVWDshoJc767.IiswtTO.k2Da",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuKoPPDLo0xPKydE2qLbxMp.AIXP.mZay",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJug/ZRZ8K2oZVoMBtPmSgiVFcvJ9Gq16y",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuVZgC3poKh5jfKEibk7q4wBYBEwlxeXa",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuIyar5GBWP2vdcb5Oy4oFy/PbRGwetEO",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuRICKOPlbeMTuq2dLTzF/DgpDAYxe/w.",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJum.YgaEL.J1PJEMimV6GzZzT2.s3vGaG",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJua/pQ2EMoI9t3OjOMGVABj1OVapVg1rS",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuM0yW/E46616vtCeh7NyjHja2ZrG1zN.",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuDNcjhUUxlmb3Fijg8DZCa0TPmboD1Y6",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuApYxs05uvaXrcFltbsyOMS92zxrF4fq",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJue3dGxN0aBf7CYFwwYI4aDRQIcyTZWh2",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuJ90/8LNmEJg7c18P58NWed86Q2Rp.8K",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuEx.Pl4ag/vNw4L6kUUC7GjlIpSvQ4ja",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJupuS/7N413cyEZ24S9Gw.b2BPcOfE2uG",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJunD0hQ7zXVo044OWtfs1ObEEgjnacMxe",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJu1mVRLYs5MfU3Ccy0MOhxeTqdmGi4KrO",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJumdkSp2GXCFXJZUba5hOItii04kIM9S2",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuxEdUM7zLkNGK1S724eLs2deedE0ZPYi",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuTbpO9pmEBoYGMYqbAvrGnnbcCEjbKIy",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuPc.YU0eF63rTg8poKCbwEDbyl8Mfghy",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuU5ZQyfUL8zSfbHIFqZr6Kp453Jn4oe6",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJul4knSearyKnX0H6x/Qxidsp0bIo5uIq",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuKAgcNSUYMSKLWXBAety4x5APnAjVX3C",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuvsw7TgIfCBljiQVlbRUJpGIewSh41P2",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuaMm5W3vIQRfDbW3Puv/RsCmQpERu3gi",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuXbpdUFHCvFwsiV5jxWTqyavac6V9SlW",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJukjpNLoBONSCNPRiiRj6ZOM3UzCZ17cO",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuQNps82pO3iQngsrUmcd2sQy2iacn62S",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJu4/I/LOOKP9ZvDoOtSO83EmSPylYJpFi",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuAgnzehFer1FiiOWilqfRvMGq8Kozxdq",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuXm/0Lu.zCG5PJ/kRkhqdvdvbVXg7s4u",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuSheI6/aS1XJPoAzMDD3s5qrT1IPSsAm",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuJvCzAr74AJDO31.yuHuIUmCU1tOhrha",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJusB/XhsN0ZquWFYtk2wprLG2qk7N5kYa",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJumdQMs/dNNvYDLfydQX6QogZaTssvbKO",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJu5kaMWYAdzSy/dnIikI0ht0OKPLXSs.u",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJu5qzEDVsfoNoOVwMGg1074Y7kktm1NCW",
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJuRUNE8Lf99ff4Y9AkeYncJUxEJBP5Ray",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uYm5OIST.9tNlp3jKcqOnJqyjva/Rf9K",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uEt/AYmpF6XovIE3o8mStD7vXrq3z3xy",
                "$2a$10$6wvKvWps59VEQHIQhTRA0ux5j0/xi/pXA0abQTyv91CoWI75T1hyu",
                "$2a$10$6wvKvWps59VEQHIQhTRA0ugjel.aaSV3odSr3Qsi58DB0Ve5X/Nvm",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uBnvwr2hLN2LUuRXCsXwPWfhq3QXDZNW",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uRsuawKCdMrRPQpJtc49GA1ETWu9ieWu",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uPGQNvQNbxHH3qF2v3RZmXAPxTjQPnwW",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uxzHnI9NBAmEme/cyRg7SNih4DFIPDCW",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uUQZ.1c.doa49/MX7Xevcolgm6v5ftS6",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u3UrAcMxNlTdDatsQPZGIhbVNU/m7DNC",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u.z1qDZQZbQ1323irM3jYEdTnPE3Lc.a",
                "$2a$10$6wvKvWps59VEQHIQhTRA0ud/HP0PHVlvn7SuWYJiZic6muz2OnErm",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uFIIheWnNMHTjIzf5.9jEx67edPHSxvy",
                "$2a$10$6wvKvWps59VEQHIQhTRA0um1j2OlCGwokbck4FZGW41cNmbGcLOz.",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uqxFFESwIvm5BRKlBjz5qezKVYdq.2b6",
                "$2a$10$6wvKvWps59VEQHIQhTRA0ufNRal9nflAami6yJ4BloGwGcsC8ua1K",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u/oskA3exViPDUBTVRzoBHXWjOTzIHD6",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uqPLZpiegcuAtZoa2opJXAZ9PI1JG41O",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u6B.TeH1Yxh/JdVnFoZ7vr6VOhi1lLze",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uxCwOIxrXeS6Oq7CMcrEnFwBqjGwLMyO",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u6BdR0LDVWHwulS4xCUjHAJRhVmEq.U.",
                "$2a$10$6wvKvWps59VEQHIQhTRA0ubA/C8ktP.OKZL7ADUP.JpH4ra/qaNoq",
                "$2a$10$6wvKvWps59VEQHIQhTRA0up.iEx5zO7gEBKhH8YBMRFJKUFR3DgfG",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uil06POdmGBsWdbid.E3VLOvsr5x8AF.",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uRwTSDWsC09AK07d7Km.nSBe9NpBEIdC",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uZ9dW4DRaCO8hZq.dqgim7hJ34LyQeB6",
                "$2a$10$6wvKvWps59VEQHIQhTRA0udG2WPeYzGnVoGV8cFN4DvHu47lrkbpK",
                "$2a$10$6wvKvWps59VEQHIQhTRA0ug6wzGTGF61oMC.uEOgpNpVaDOqVYJRG",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u788hVWnNrCe434RGGOgg0ok3kGofAmO",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uyevhcbtt1l2Z2WV/CTm2XElarzpXLwq",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uk6Tuue2rVRrpfiVMxYlkn26G.dVOPfi",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uFXg22TgpbY6VutJluQJ9/tgxEfj.0o2",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uLpwitcJXwCajL1XKVCrGfGtrR23JAUa",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uxLievsYCuBPYy2OM6Wm2UafD6fao8kC",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uPZ3NbCPSw.XitLWxLSqBqeJB2pH4uPq",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u1f.Vgxc0eLgfI4/Bps4byxF/ISqtmAy",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u0NkEqXF1mKrxd/u6vzVR3f/p4cEm7sq",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uR9d1QqpSTt9HtcSv7pjMBqqBNAawzO2",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u7X9KxQae40OfJL0JenQi7Fhs7z5rEMW",
                "$2a$10$6wvKvWps59VEQHIQhTRA0uHdZOBVhTVmIM6EpSxJjBnbPrTm/u2cC",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.u29u1cN.kb4WqR..y3fgvFVgzH783A6",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.vewB6tK3itgu5tXWn128EhPTkmMnkFy",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.WzHnXVEvF5eIyEJ/8BJslvrA0XhRQXm",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.BvGk2ZYJPFKWgj.P/BYcEH5Ego2r0dG",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.0alPgotTFgiSyZtGRFsOaasRKBLNF7O",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.YmSk1q7e/wfd.k3v6TO7aqHOU7n24Qq",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.jBXzUsJPy4HSiVPjhrS/g95gFvvLG6m",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.cfn0zWODdqwyHiZeyMk.ibgc8wicAla",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.qtxJ.3GS.WLbhN4kPQB0ByUPcBXOT2W",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.qz/57frJeY9..4BT2bSTVNMxZhihjbS",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.Vg4Nidqm6Y0KAZ/Q6qasQtd6YXv8hDy",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.QDCasfC7xOb3/NEf/fZpB4kRH1YfgZu",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.EtwDjMJDNZp6BAJElAKdDr9ePr4mUrC",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.hmfazhUTiBvbaN32gjm2n1OOUJ9vteK",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.FO.Oj5Tj7LPi010Vjh6gWboAKgCyHWS",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.82kqiJUvrYbshdSNlgu0VCSPBXT.uM2",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.GxhPFRA4kKUpJPEV9KSdBzb9PauQGgK",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.tgCTCa6eu.aIIknK12mTru14ofVXPaO",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.PYCRxQnoOLqo4OsLlKZFsPOU5FvHR4.",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.c4oJEoVwzqHrQx7W2UC7x0eM1UrPLl.",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.VMsfj3uLAhIE8A6e/zin4SbGBnbYAWa",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.k3oSv3vio6yTQo/7h578r1VD6rB25km",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.t0.azQuhHIkD49pp/3stkv4Nb9Bztx2",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.OJBTBZk2EOSPlxT0ZfEAX2IGv.O6mvq",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.0ywRgItNbm29ANSOyt3/6OLR6wp4knu",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.4A4JHqLEOKtST6GT9Kb0JEFkEcloINO",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.jJ4rgoD4NDvgHfO9IntRn5OnekMe7Ti",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.Do.7Ngf2hVWIW8TaIg/F3A0Lr/RreAa",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.CUQJKyiudoYpOUB.9WiuQ3/ko6DZjWq",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.e5820E6YI0KwCdqiX4MeMVu/Q6cZbwq",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.aP5E7zwt8wjRqza2XAygE0NIvnnk20a",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.H8OGKbBiWtxc9xjoEddX.mm2H0Mg9J6",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA./CDjuWvk5RG8bGRr8QPOaHVFvTigxHi",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.4MzY2KGViGlpvh4/xv8BUx7pcx06jkm",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.VECtx5QGZY0X9Cn5B5TBf8gU0UCiZqG",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.UsyI4IzRB8Rhp6jDQXbSzxnHlN810pm",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.djFXvyiOyCCnsoDxzt812fdTpKzBVbK",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.X0.g5kEgUX4xpLKIu64UAr/oma.rEsO",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.esZuxVPm478.5X9aPTpuI3qVe5HLAxy",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.3jjUUQNmks1E/VZnZXw6DpB5Og7Dxg2",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.HZlY2Ypytb5JtguQ1Qiewq8WQ56xmFy",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.4SZRMFExxH4ToN1glBgifJetZybh5lC",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.JWDW9GhmYeIuRYL8oyJtNqXCnudDaCm",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.CAfB35QAUJEyCp6X5BhBiTfmBIlVngu",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.cJ99OzKOdxhs7Ufd1xA.ZQ7nHdgrc9m",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.A39bjukexrpsqwQJBeTFpEi8U1abIuC",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.pFhUFQtuJnFnO3GmBkl64GFbAyiaAiu",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.2r/WOQrLLKFHPgaPpA3cD/ywJT9SnoO",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.HRQuZGI3GcFZsQPVX3yQ3kYHrt169e.",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.4doWpXqzN9c8Lkol/x3Vh2QmN1GzXqK",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.2IpjNHWQtXj1ZJf2lXWws3Z2Eci32F.",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.8VeedFRX2TWpy0fyLQO55UF5jRAIMh2",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.9dTAtWkQjRB0UFu0iN8O8pE/5.jwnGq",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx./gMZ5S2TJR8N1a1igF79h8tH68FjFb6",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.5Y3KMj8shyibVcFreamVwSkfoJCrjeW",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.IUndpmm6LeC5O1sJB4CaVS4/yxYSIP6",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.Op2lMIIPIayRj4Z6QL0wKcicafCFxYa",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.DrcTSapNR3k8Dxxblg.NINYLmh8tghm",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.x/sntePDuBlHR/FWi/OWVWx1QY/CwyG",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.MfYsoCcuWsAniuF763belT9Gz91b5Ka",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.1lVEjkYjFWXKYRugBxAgN2/tvOv7c1S",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.LQMbaSddnjlI29GvwesyAvt7ZT5LfBy",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.MI03OMTvbnEgYmge2l9JK43Zc3yrGxC",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.HDd..KbzjyYM1Mlqn91u0e1718XZz0y",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.9jRO9QbJN5U3h7P8C0TyXTnNZdGrrZ.",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.RxUUDAgibwxjlBw4XULPDheOCqP44N2",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.8WGtJDxLNIK80UtfqVfxp6zC9KpwZDS",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.cG/rGmhsJWHJAWnBh02evScVBjiCxLK",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.EHusHy1GQSt6AUcSwE7e4u3hh/1AQ5K",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.EVxy3qOebN1uFVzzdLJNegxyNKfSiFy",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.z2gqxtxcy13dJt.1qiELcBYSF80Iy2a",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.1hof0FId9ars2NBuQDqsC3McVcO6Qry",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.cVFDfkqynLCIrTH/YCH.W6/L1ljGhX2",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.PxPAnGkIbMedm.8HXolFilhFZuVrvHa",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.ZCNpbt0UmEB7gWJNT/ExkLww0LHnt/y",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.lEKJtsI1wnVAnc0HAMbRAsyJ0bic9i2",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.lZZx.8bNR57PK3OlUj1QOT1Cqp7fgx6",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.fg4t2zIF7esv0oe01WdZJKLgOWpAS5S",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.zgSnZ8vMViEOvctgXibcZ8N187ZpfaG",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.LqYJlW1pt5NWKQ02HusX2h0sAAudWuy",
                "$2a$10$htBrPOgmGrsF0nl83GixDO/F0fmaI5sOounKE1g48unYu5cMcXgpq",
                "$2a$10$htBrPOgmGrsF0nl83GixDOQ8DV7y//yfQMEbKtu13b6QR1cQULAX6",
                "$2a$10$htBrPOgmGrsF0nl83GixDOmZol6Dta/GPEXuLyubWlMQE088aYd.u",
                "$2a$10$htBrPOgmGrsF0nl83GixDOrbTmbB2sqWeQx.g9mWBG6euu6msFxNu",
                "$2a$10$htBrPOgmGrsF0nl83GixDOveYCf0O4ohkWyDKqdG4pkq3.gxsry3W",
                "$2a$10$htBrPOgmGrsF0nl83GixDOcqzt8O5E6R.s3JsZM0dfEbgFDtqkb.m",
                "$2a$10$htBrPOgmGrsF0nl83GixDOpPFblcaOeKU.kr0KlPl.7wF9Y8SHvyO",
                "$2a$10$htBrPOgmGrsF0nl83GixDOtnWZ6GHHmGxPS.EgF6STH5wZN3EvRhO",
                "$2a$10$htBrPOgmGrsF0nl83GixDOyexCnyelUTEiH0KLxFkZ40qECjgu/sy",
                "$2a$10$htBrPOgmGrsF0nl83GixDOhcp.ftjpWOYqgP/UC5aSt7qAcVPzPQG",
                "$2a$10$htBrPOgmGrsF0nl83GixDONUQyZ7pEkz9ihxTWrUdS.w5Dn2JxiXm",
                "$2a$10$htBrPOgmGrsF0nl83GixDO2PNgLp.uLrD8RVyMo7yGSdlxCmn2Kcy",
                "$2a$10$htBrPOgmGrsF0nl83GixDOHyvSmVw5i4jG433tO7uhQzQlUlAKwpa",
                "$2a$10$htBrPOgmGrsF0nl83GixDO2wuFPAf97FKQGfHdrdaI61ZUiYAa1ti",
                "$2a$10$htBrPOgmGrsF0nl83GixDO6eMN3VtgvdYnQb07IhmkWMzohDn6jpS",
                "$2a$10$htBrPOgmGrsF0nl83GixDOZmeWbrlmGQ7cl5QGyPTEEbBe4Uc7oGK",
                "$2a$10$htBrPOgmGrsF0nl83GixDOURXfWmLDnff8iYI.wnuiY/tzLU2oA9y",
                "$2a$10$htBrPOgmGrsF0nl83GixDOYj8jJwb83L6C6Jv2GvM/LyG0.G1MrsG",
                "$2a$10$htBrPOgmGrsF0nl83GixDOh9nK.knpNPfxt9NlANvJnlIBr4o2ziW",
                "$2a$10$htBrPOgmGrsF0nl83GixDOoI3d6GwLGBVETiZWK9W7heHqCKDuIQy",
                "$2a$10$htBrPOgmGrsF0nl83GixDOaxq65iWaFY6o3qq.cFzZI0XrAM8YsFG",
                "$2a$10$htBrPOgmGrsF0nl83GixDO3/yZJMnsadjdPIrYrKoPerdUZrBUPGC",
                "$2a$10$htBrPOgmGrsF0nl83GixDOdoGd8BkLTfmFkBtV4uAhlyVAZqMqEki",
                "$2a$10$htBrPOgmGrsF0nl83GixDOzFua.lSEelUXVsXShnTj9BqbLBTMlVm",
                "$2a$10$htBrPOgmGrsF0nl83GixDOBm3mznYEB3uOJYdr6SO4rtRVCQ.wXoS",
                "$2a$10$htBrPOgmGrsF0nl83GixDO4wapGS7QYLq359T0V0.v5y5..sQo.1S",
                "$2a$10$htBrPOgmGrsF0nl83GixDO6dJM4vK.sSK4GGmn2TPhlsH2QHhBxOi",
                "$2a$10$htBrPOgmGrsF0nl83GixDOPyNS2BGAbh/nK/Sytm8cIdN0TX73s9i",
                "$2a$10$htBrPOgmGrsF0nl83GixDO8D1FqZUQHUXwHhKsbYqi0qVcK555vEm",
                "$2a$10$htBrPOgmGrsF0nl83GixDOmBV8SFZQuhEcdcCEqt7GZPK3qE8LgCi",
                "$2a$10$htBrPOgmGrsF0nl83GixDODqSR7r19AVox19lX5LbEDzO07/UjFeC",
                "$2a$10$htBrPOgmGrsF0nl83GixDOl0OsCELeAnbZh8w4JHZWn1DgW1sbuHe",
                "$2a$10$htBrPOgmGrsF0nl83GixDO3ZKnKzRf7oI1IwOlovmIzcdGEQWB4gi",
                "$2a$10$htBrPOgmGrsF0nl83GixDOFINeNoPENj4Gk119uJpnrZ81KClgu8q",
                "$2a$10$htBrPOgmGrsF0nl83GixDOR1/vEAHAZK8pEPw8c4960B6p3as4ooi",
                "$2a$10$htBrPOgmGrsF0nl83GixDONnh6tiq2VTkiMcMS6/T1IkKDRgQRlzW",
                "$2a$10$htBrPOgmGrsF0nl83GixDO0prEup98tJkp8sXdO0rOwTQhlGop3za",
                "$2a$10$htBrPOgmGrsF0nl83GixDOqsUeBE/pR/nWiFfnieJdUkROvc7h6Hq",
                "$2a$10$htBrPOgmGrsF0nl83GixDOPwFit6k0acvq.P1XKbzLCyg7GXWnj86",
                "$2a$10$htBrPOgmGrsF0nl83GixDOdIEQEp367edUCQ73Gj0Cy1MZnU70N5m"
        ));
    }

    List<String> SaltList() {
        return new ArrayList<>(Arrays.asList(
                "$2a$10$rOSPFvf8MKrq6YEsP.zVJu",
                "$2a$10$6wvKvWps59VEQHIQhTRA0u",
                "$2a$10$lE70eCDXb4pKzZJ2c8cUA.",
                "$2a$10$Fua.2BAIOwaO7EI6f.RPx.",
                "$2a$10$htBrPOgmGrsF0nl83GixDO"
        ));
    }

}