package in.tubalaw.courtos.modules.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        System.out.println("GENHASH:" + encoder.encode("Welcome@123"));
    }
}
