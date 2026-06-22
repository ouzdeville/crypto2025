import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

public class Test{

    public static void main(String[] args) throws Exception {

        Security.addProvider(new BouncyCastleFipsProvider());

        AlgorithmParameterGenerator paramGen =
            AlgorithmParameterGenerator.getInstance("DSA", "BCFIPS");

        paramGen.init(2048); // FIPS compliant

        AlgorithmParameters params = paramGen.generateParameters();
        
        DSAParameterSpec dsaSpec =params.getParameterSpec(DSAParameterSpec.class);

        System.out.println("p=" + dsaSpec.getP());
        System.out.println("q=" + dsaSpec.getQ());
        System.out.println("g=" + dsaSpec.getG());

        KeyPairGenerator kp=KeyPairGenerator.getInstance("DSA", "BCFIPS");
        kp.initialize(dsaSpec);
        
        kp.generateKeyPair();
    }
}