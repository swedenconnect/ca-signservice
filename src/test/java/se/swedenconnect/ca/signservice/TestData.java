/*
 * Copyright 2024.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.ca.signservice;

import org.bouncycastle.util.encoders.Base64;

public class TestData {

  public static byte[] getCertBytes(String b64CertStr){
    return Base64.decode(b64CertStr);
  }

  public static final String CERT_1 = "MIIKBDCCCGygAwIBAgIQLlyLtuKN+eoz2td/4RnAiTANBgkqhkiG9w0BAQsFADCB\n"
    + "hzELMAkGA1UEBhMCU0UxFjAUBgNVBAoTDVN3ZWRlbkNvbm5lY3QxGDAWBgNVBAsT\n"
    + "D1NpZ25pbmcgU2VydmljZTEVMBMGA1UEBRMMc2Mtb3JnTnVtYmVyMS8wLQYDVQQD\n"
    + "EyZDQTAwMSBTd2VkZW4gQ29ubmVjdCBURVNUIFNpZ24gU2VydmljZTAeFw0yMjA4\n"
    + "MTYyMTIwMDRaFw0yMzA4MTYyMTIwMDRaMFwxFTATBgNVBAUTDDE5NTIwNzMwNjg4\n"
    + "NjELMAkGA1UEBhMCU0UxDzANBgNVBCoTBk1hamxpczEOMAwGA1UEBBMFTWVkaW4x\n"
    + "FTATBgNVBAMTDE1hamxpcyBNZWRpbjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IA\n"
    + "BJW0fHH5k0thiaKY3jXxcIS/7bLegsCW+DcWOPJCYezpkC9tz++ahzwMDsP61Bnv\n"
    + "iU3dRdH2Rggx2wM4p8w85dejggbfMIIG2zALBgNVHQ8EBAMCBkAwHQYDVR0OBBYE\n"
    + "FHhi0ggZ2Ob5CPvTubq0bxq7piX2MBMGA1UdIAQMMAowCAYGBACLMAEBMGEGA1Ud\n"
    + "HwRaMFgwVqBUoFKGUGh0dHBzOi8vc2lnLnNhbmRib3guc3dlZGVuY29ubmVjdC5z\n"
    + "ZS9zaWdzZXJ2aWNlL3B1Ymxpc2gvY3JsLzVlNzEyNjk2YWFjYTkwZTAuY3JsMIIG\n"
    + "BwYHKoVwgUkFAQSCBfowggX2MIIF8gwraHR0cDovL2lkLmVsZWduYW1uZGVuLnNl\n"
    + "L2F1dGgtY29udC8xLjAvc2FjaQyCBcE8c2FjaTpTQU1MQXV0aENvbnRleHQgeG1s\n"
    + "bnM6c2FjaT0iaHR0cDovL2lkLmVsZWduYW1uZGVuLnNlL2F1dGgtY29udC8xLjAv\n"
    + "c2FjaSI+PHNhY2k6QXV0aENvbnRleHRJbmZvIElkZW50aXR5UHJvdmlkZXI9Imh0\n"
    + "dHA6Ly9kZXYudGVzdC5zd2VkZW5jb25uZWN0LnNlL2lkcCIgQXV0aGVudGljYXRp\n"
    + "b25JbnN0YW50PSIyMDIyLTA4LTE2VDIxOjMwOjA0LjAwMFoiIFNlcnZpY2VJRD0i\n"
    + "U2lnbmF0dXJlIFNlcnZpY2UiIEF1dGhuQ29udGV4dENsYXNzUmVmPSJodHRwOi8v\n"
    + "aWQuZWxlZ25hbW5kZW4uc2UvbG9hLzEuMC9sb2EzIiBBc3NlcnRpb25SZWY9Il82\n"
    + "NmJjMTQ1Y2U0YzgwYmI2YTdkZDhiM2M3ZjU4NzQ0ZCIvPjxzYWNpOklkQXR0cmli\n"
    + "dXRlcz48c2FjaTpBdHRyaWJ1dGVNYXBwaW5nIFR5cGU9InJkbiIgUmVmPSIyLjUu\n"
    + "NC41Ij48c2FtbDpBdHRyaWJ1dGUgRnJpZW5kbHlOYW1lPSJTd2VkaXNoIFBlcnNv\n"
    + "bm51bW1lciIgTmFtZT0idXJuOm9pZDoxLjIuNzUyLjI5LjQuMTMiIHhtbG5zOnNh\n"
    + "bWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPjxzYW1s\n"
    + "OkF0dHJpYnV0ZVZhbHVlPjE5NTIwNzMwNjg4Njwvc2FtbDpBdHRyaWJ1dGVWYWx1\n"
    + "ZT48L3NhbWw6QXR0cmlidXRlPjwvc2FjaTpBdHRyaWJ1dGVNYXBwaW5nPjxzYWNp\n"
    + "OkF0dHJpYnV0ZU1hcHBpbmcgVHlwZT0icmRuIiBSZWY9IjIuNS40LjQyIj48c2Ft\n"
    + "bDpBdHRyaWJ1dGUgRnJpZW5kbHlOYW1lPSJHaXZlbiBOYW1lIiBOYW1lPSJ1cm46\n"
    + "b2lkOjIuNS40LjQyIiB4bWxuczpzYW1sPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FN\n"
    + "TDoyLjA6YXNzZXJ0aW9uIj48c2FtbDpBdHRyaWJ1dGVWYWx1ZT5NYWpsaXM8L3Nh\n"
    + "bWw6QXR0cmlidXRlVmFsdWU+PC9zYW1sOkF0dHJpYnV0ZT48L3NhY2k6QXR0cmli\n"
    + "dXRlTWFwcGluZz48c2FjaTpBdHRyaWJ1dGVNYXBwaW5nIFR5cGU9InJkbiIgUmVm\n"
    + "PSIyLjUuNC40Ij48c2FtbDpBdHRyaWJ1dGUgRnJpZW5kbHlOYW1lPSJTdXJuYW1l\n"
    + "IiBOYW1lPSJ1cm46b2lkOjIuNS40LjQiIHhtbG5zOnNhbWw9InVybjpvYXNpczpu\n"
    + "YW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPjxzYW1sOkF0dHJpYnV0ZVZhbHVl\n"
    + "Pk1lZGluPC9zYW1sOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDpBdHRyaWJ1dGU+PC9z\n"
    + "YWNpOkF0dHJpYnV0ZU1hcHBpbmc+PHNhY2k6QXR0cmlidXRlTWFwcGluZyBUeXBl\n"
    + "PSJyZG4iIFJlZj0iMi41LjQuMyI+PHNhbWw6QXR0cmlidXRlIEZyaWVuZGx5TmFt\n"
    + "ZT0iRGlzcGxheSBOYW1lIiBOYW1lPSJ1cm46b2lkOjIuMTYuODQwLjEuMTEzNzMw\n"
    + "LjMuMS4yNDEiIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIu\n"
    + "MDphc3NlcnRpb24iPjxzYW1sOkF0dHJpYnV0ZVZhbHVlPk1hamxpcyBNZWRpbjwv\n"
    + "c2FtbDpBdHRyaWJ1dGVWYWx1ZT48L3NhbWw6QXR0cmlidXRlPjwvc2FjaTpBdHRy\n"
    + "aWJ1dGVNYXBwaW5nPjwvc2FjaTpJZEF0dHJpYnV0ZXM+PC9zYWNpOlNBTUxBdXRo\n"
    + "Q29udGV4dD4wCQYDVR0TBAIwADAfBgNVHSMEGDAWgBSUvCNhmNvK475lngc4KV8V\n"
    + "npcIljANBgkqhkiG9w0BAQsFAAOCAYEATYJKaSQu2UHiLL/JnS/JGFCnAlGG+DYM\n"
    + "XJdwLv7GX2vnABhewaRgqLqgoKwtfpVuU6LbeASDByObbrgW+LsO5h+isoLipUo7\n"
    + "bh7V17YANdo2uVWpBGV0W6LzurQnGMmj23fD0K0ZUmFnhBlRc6lDGbFMfM1h49rM\n"
    + "s1Kj9ss6EWCLCNZV+JwuuIGp9I5ROJk/2S+TNuq5mYODVDd0Bd1hmoQMBObpNJp1\n"
    + "fzup3Tvidp2AaquOAvytqZH0x20a9hqOU5cmVuS7v8UtHKPDUhTZy2BZSU0AZsW5\n"
    + "sQ7h8EwkqloQxCMbvIlUnoMhn9+k8JPblBvhIe9wTGbBdLUgGOaQ/qkSOjaVH4V/\n"
    + "dhY0/Gmk5rDH9gsvPk4or5iqK+KMYeYFtAC5l2sEcmLZKqvx2m+8ulAwFmiOHQ5r\n"
    + "oE028Pg8lpneHZdWamYuqSxFP35ICoa062XmCZp9g00qplvvfiHatyQILEL84WoG\n"
    + "IWbRFSMiagAr4aqUNeOfTPzAo5wkhJ+q";

  public static final String CERT_2 = "MIIKIDCCCIigAwIBAgIQCg4fMpe8PtyIO13E9WAwUjANBgkqhkiG9w0BAQsFADCB\n"
    + "hzELMAkGA1UEBhMCU0UxFjAUBgNVBAoTDVN3ZWRlbkNvbm5lY3QxGDAWBgNVBAsT\n"
    + "D1NpZ25pbmcgU2VydmljZTEVMBMGA1UEBRMMc2Mtb3JnTnVtYmVyMS8wLQYDVQQD\n"
    + "EyZDQTAwMSBTd2VkZW4gQ29ubmVjdCBURVNUIFNpZ24gU2VydmljZTAeFw0yMjA4\n"
    + "MTYyMTIxMTVaFw0yMzA4MTYyMTIxMTVaMGoxFTATBgNVBAUTDDE5NzgwMjAzMTg3\n"
    + "NzELMAkGA1UEBhMCU0UxEDAOBgNVBCoTB1RyeWdndmUxFDASBgNVBAQMC0LDpGNr\n"
    + "c3Ryw7ZtMRwwGgYDVQQDDBNUcnlnZ3ZlIELDpGNrc3Ryw7ZtMFkwEwYHKoZIzj0C\n"
    + "AQYIKoZIzj0DAQcDQgAEX9lntuuWQtd1v1BQJACiuOY8YLzFXq0AiLU2JZFaGJjU\n"
    + "DPdLKUDxIUEtzRfyWTEkKrY3RKv71ReuZWmaYBfbWKOCBu0wggbpMAsGA1UdDwQE\n"
    + "AwIGQDAdBgNVHQ4EFgQUtjtXx/708OI3VnXK3BZnEzmrIp0wEwYDVR0gBAwwCjAI\n"
    + "BgYEAIswAQEwYQYDVR0fBFowWDBWoFSgUoZQaHR0cHM6Ly9zaWcuc2FuZGJveC5z\n"
    + "d2VkZW5jb25uZWN0LnNlL3NpZ3NlcnZpY2UvcHVibGlzaC9jcmwvNWU3MTI2OTZh\n"
    + "YWNhOTBlMC5jcmwwggYVBgcqhXCBSQUBBIIGCDCCBgQwggYADCtodHRwOi8vaWQu\n"
    + "ZWxlZ25hbW5kZW4uc2UvYXV0aC1jb250LzEuMC9zYWNpDIIFzzxzYWNpOlNBTUxB\n"
    + "dXRoQ29udGV4dCB4bWxuczpzYWNpPSJodHRwOi8vaWQuZWxlZ25hbW5kZW4uc2Uv\n"
    + "YXV0aC1jb250LzEuMC9zYWNpIj48c2FjaTpBdXRoQ29udGV4dEluZm8gSWRlbnRp\n"
    + "dHlQcm92aWRlcj0iaHR0cDovL2Rldi50ZXN0LnN3ZWRlbmNvbm5lY3Quc2UvaWRw\n"
    + "IiBBdXRoZW50aWNhdGlvbkluc3RhbnQ9IjIwMjItMDgtMTZUMjE6MzE6MTUuMDAw\n"
    + "WiIgU2VydmljZUlEPSJTaWduYXR1cmUgU2VydmljZSIgQXV0aG5Db250ZXh0Q2xh\n"
    + "c3NSZWY9Imh0dHA6Ly9pZC5lbGVnbmFtbmRlbi5zZS9sb2EvMS4wL2xvYTMiIEFz\n"
    + "c2VydGlvblJlZj0iX2ZjNjRkNDY0MzY2OTJiODliYWU3YjE4MDgzMzliY2FmIi8+\n"
    + "PHNhY2k6SWRBdHRyaWJ1dGVzPjxzYWNpOkF0dHJpYnV0ZU1hcHBpbmcgVHlwZT0i\n"
    + "cmRuIiBSZWY9IjIuNS40LjUiPjxzYW1sOkF0dHJpYnV0ZSBGcmllbmRseU5hbWU9\n"
    + "IlN3ZWRpc2ggUGVyc29ubnVtbWVyIiBOYW1lPSJ1cm46b2lkOjEuMi43NTIuMjku\n"
    + "NC4xMyIgeG1sbnM6c2FtbD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFz\n"
    + "c2VydGlvbiI+PHNhbWw6QXR0cmlidXRlVmFsdWU+MTk3ODAyMDMxODc3PC9zYW1s\n"
    + "OkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDpBdHRyaWJ1dGU+PC9zYWNpOkF0dHJpYnV0\n"
    + "ZU1hcHBpbmc+PHNhY2k6QXR0cmlidXRlTWFwcGluZyBUeXBlPSJyZG4iIFJlZj0i\n"
    + "Mi41LjQuNDIiPjxzYW1sOkF0dHJpYnV0ZSBGcmllbmRseU5hbWU9IkdpdmVuIE5h\n"
    + "bWUiIE5hbWU9InVybjpvaWQ6Mi41LjQuNDIiIHhtbG5zOnNhbWw9InVybjpvYXNp\n"
    + "czpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPjxzYW1sOkF0dHJpYnV0ZVZh\n"
    + "bHVlPlRyeWdndmU8L3NhbWw6QXR0cmlidXRlVmFsdWU+PC9zYW1sOkF0dHJpYnV0\n"
    + "ZT48L3NhY2k6QXR0cmlidXRlTWFwcGluZz48c2FjaTpBdHRyaWJ1dGVNYXBwaW5n\n"
    + "IFR5cGU9InJkbiIgUmVmPSIyLjUuNC40Ij48c2FtbDpBdHRyaWJ1dGUgRnJpZW5k\n"
    + "bHlOYW1lPSJTdXJuYW1lIiBOYW1lPSJ1cm46b2lkOjIuNS40LjQiIHhtbG5zOnNh\n"
    + "bWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPjxzYW1s\n"
    + "OkF0dHJpYnV0ZVZhbHVlPkLDpGNrc3Ryw7ZtPC9zYW1sOkF0dHJpYnV0ZVZhbHVl\n"
    + "Pjwvc2FtbDpBdHRyaWJ1dGU+PC9zYWNpOkF0dHJpYnV0ZU1hcHBpbmc+PHNhY2k6\n"
    + "QXR0cmlidXRlTWFwcGluZyBUeXBlPSJyZG4iIFJlZj0iMi41LjQuMyI+PHNhbWw6\n"
    + "QXR0cmlidXRlIEZyaWVuZGx5TmFtZT0iRGlzcGxheSBOYW1lIiBOYW1lPSJ1cm46\n"
    + "b2lkOjIuMTYuODQwLjEuMTEzNzMwLjMuMS4yNDEiIHhtbG5zOnNhbWw9InVybjpv\n"
    + "YXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPjxzYW1sOkF0dHJpYnV0\n"
    + "ZVZhbHVlPlRyeWdndmUgQsOkY2tzdHLDtm08L3NhbWw6QXR0cmlidXRlVmFsdWU+\n"
    + "PC9zYW1sOkF0dHJpYnV0ZT48L3NhY2k6QXR0cmlidXRlTWFwcGluZz48L3NhY2k6\n"
    + "SWRBdHRyaWJ1dGVzPjwvc2FjaTpTQU1MQXV0aENvbnRleHQ+MAkGA1UdEwQCMAAw\n"
    + "HwYDVR0jBBgwFoAUlLwjYZjbyuO+ZZ4HOClfFZ6XCJYwDQYJKoZIhvcNAQELBQAD\n"
    + "ggGBAAdssSyVUUIw/Wp4U81SvJUG7vkVDEloyeSKhIP04XDfHFFy7ja2QZy+nRGg\n"
    + "y1kN/s2Xs05HJwBcJn4zHVMmYcuyiPJV83GhBF5oLViPu2HRXPq+BLJboHMf5TSs\n"
    + "563hja3yK71jAc5thZh7GhPvtDAOqxar+ui01y/IiCGyAtPMabd4FqkXYlXRPmp2\n"
    + "q6JCQP7Wb6OfnryeicjlW0Vadz4EneczGfR579yEi/bH/IL3cRWzbzzqrra0ja6c\n"
    + "M0MDaAc4VGDXURCftsQMyXTmtzKTYhpIJ5rs9/42XLDT64wJTHNvj9QX8DXTyP16\n"
    + "vNIJ4RCDEyxaudS7TGZjeaP50wV2pVDVuCM98KtATULgd2dkqqDRmnBFMXGaUcjC\n"
    + "m9FkAoplA6NZeQuql8AIqHVBzJJq7VpEXI2WYuy72K0gM37XKGpHvSCEsYkRpwt9\n"
    + "bS/OwtU6woQg9cRB/7aQURkW9+7Ye0JaonI6A8p8miqsiL4g8Jyp+f5k91UmwX6i\n"
    + "jvxutg==";

}
