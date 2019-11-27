package cn.erika;

public class T1 {
    public static void main(String[] args) throws Exception {
//        System.out.println(AES.randomPassword(255));
        /*String aes = "'h,C[Z+&";
        String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCDfipS2vBNyzdCKQc7B8uYoAK+4kM/n2uN35cdUWq2+qaDWU0yqDjwSJhEij7WF+vLmlJONOahKtsK2nu5bVBaPueAz230Uzk730xrcbGsLS22niUhfUY31BAyOa9ok5ml4bZzo0Epw4uqfJ1YZUg5W+U8OpDgxp/GZfn/aLPuBN3eQKYv1bxg+pc+MWMVbui+bTUrs7NO8ioWsIVWTrl+a2CVgEz8wo/wcz38WR8N7QibKhv2Y90VvfKhbAzZyNVuZNPizOifVrj2cQJ0X4Y5qG2rYJTmwOigGRXYcEns7dPLgDJO1EoQ/ORKRYOly6CYlZO/X658nH5cH23JkcoVAgMBAAECggEACiRuIUvv4yA8l+bQPvZf9trIu256X2rtgdjwL/u6rnhw5Yq60KB3G9mNnSPIqvzXXDIADTY5X88NbC3kAy5yVzOo6Z+WTJUqwfp8S4FOgghOEVrrZETkaKP7x9P5Oqi/3jKlvw+llBIYMMNB47KUOpBjAx1FVYEci9IL0W6DCmXLNpOdCNiBdzPLG56rj1dxTABLadyNbdqQJ4YmXJ+6CFAL/0qny880sLyvTu8O2Xsw9piKzPUpQCa3KlVgkQR93Lgcr6yeHklQdznusPv8YFmQrQqBcvzrk93twg/IdGwi0BpSWbZNGYq3k9uRgaTToc7rb5vLKk5B0GFg8vNEoQKBgQDNk3uhhBklIBBzKcIV0V+5qqcgQN5oyG906qf4WcOHG+v8By9v003ELdprn79dMobfEgb/PlCP8ia//fxogG45rwyc9ifiglHgq9q4gseU3BAMCBbz4JEr9qAozpByW/m0aLkGl/06AWpSW9gzr1JItFcA/crR1nxlF7wjr3s3LQKBgQCjvtoa+ysG/JWNMZDRfnNSwg6H3m1oYloXdywOpNNhYSBSAONEpwzz30rhRSJnINQV5aLdPPbhTe4IzgzKxFwIULDroRHqq3SgWqvd7TQ1aaa2SDjxUaAvrtCbmvBgSq6T9CrDL307UV3yLKjmkGlgd/4XqsfR9U89v5ejN4AviQKBgQC09WL5jUd5F52DUjnHbNN/owmUaaWdxPnfiAOtqI5mKxzyb/zAydcFnntLC4au6iAw0jSYj+jonPahQFjkQFFGs6E9tazsRVz/kSdqDazTi1cQ0DosGPyRH3piEVqzX+URerGMbVP5sG8/6hsJfAXJe/2uILldNxmu/1KccwbaTQKBgHRdE4frCAe+BdVB7QVDVFbyevsJET0F77oPQWrirSjdqec8pEVMZYMFIqR1yd350V4CQ7vvMA6qn8b4DtUlt7VdCm1YmG/pCkdOSCa9A8YVX5jYOEcd17UrCSobcEktqWSRNrZgkCFQUG/iVuEKrSgV/ab6AWhuaLKb7fnaXijpAoGBALhsQO3KBsuvLZFU/X/akJtjdZN5cA4szaVIFWE3XoSwL5Jj2OCda0/iXnu8EwdCUuj3d9CT9ZnHw6jtS2w2qATYeaNDOpam37aSUdFQmAmDrtb6J3R0pcDa8GKQ4PD7NoO0uyYcBqTF0PXTTFpO9s4lGCilI0XFFESti/O3Mq/C";
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg34qUtrwTcs3QikHOwfLmKACvuJDP59rjd+XHVFqtvqmg1lNMqg48EiYRIo+1hfry5pSTjTmoSrbCtp7uW1QWj7ngM9t9FM5O99Ma3GxrC0ttp4lIX1GN9QQMjmvaJOZpeG2c6NBKcOLqnydWGVIOVvlPDqQ4MafxmX5/2iz7gTd3kCmL9W8YPqXPjFjFW7ovm01K7OzTvIqFrCFVk65fmtglYBM/MKP8HM9/FkfDe0Imyob9mPdFb3yoWwM2cjVbmTT4szon1a49nECdF+GOahtq2CU5sDooBkV2HBJ7O3Ty4AyTtRKEPzkSkWDpcugmJWTv1+ufJx+XB9tyZHKFQIDAQAB";

        byte[] pri = Base64.getDecoder().decode(privateKey);
        byte[] pub = Base64.getDecoder().decode(publicKey);


        String str1 = Base64.getEncoder().encodeToString(RSA.encryptByPublicKey(aes.getBytes("UTF-8"), RSA.getPublicKey(pub)));

        byte[] str2 = Base64.getDecoder().decode(str1);
        byte[] str3 = RSA.decryptByPrivateKey(str2, RSA.getPrivateKey(pri));

        String dst = new String(str3, "UTF-8");
        System.out.println(dst);*/

        /*String str1 = "123456";
        String str2 = "123456";
        String str3 = new String("123456");
        System.out.println(str1 == str2);
        System.out.println(str1.equals(str2));

        System.out.println(str1 == str3);
        System.out.println(str1.equals(str3));*/

        System.out.println(Integer.MAX_VALUE);
        System.out.println(Integer.MAX_VALUE / 1024);
        System.out.println(Integer.MAX_VALUE / 1024 / 1024);
    }
}
