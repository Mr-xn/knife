package config;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态变量函数处理类，支持在header值中使用内置函数。
 * 例如：{rand_ip(private,2)} 会生成两个私有IP地址，以逗号分隔。
 */
public class DynamicFunctions {

    private static final Random random = new Random();

    public static final String RAND_IP_PRIVATE = "private";
    public static final String RAND_IP_PUBLIC = "public";
    public static final String RAND_IP_RANDOM = "random";

    // 匹配 {rand_ip()} 或 {rand_ip(type)} 或 {rand_ip(type,count)} 等形式
    private static final Pattern RAND_IP_PATTERN = Pattern.compile(
            "\\{rand_ip\\(([^)]*)\\)\\}", Pattern.CASE_INSENSITIVE
    );

    /**
     * 处理字符串中的动态函数调用，替换为实际值。
     *
     * @param value 包含动态函数占位符的字符串
     * @return 替换后的字符串
     */
public static String process(String value) {
        if (value == null) {
            return null;
        }
        // quick pre-check (case-insensitive) before running regex
        if (!value.toLowerCase().contains("{rand_ip(")) {
            return value;
        }
        Matcher m = RAND_IP_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String args = m.group(1).trim();
            String replacement = generateRandIpReplacement(args);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 根据参数生成一个或多个随机IP地址。
     *
     * @param args 参数字符串，例如 "private,2" 或 "public" 或 ""
     * @return 逗号+空格分隔的IP地址字符串
     */
    private static String generateRandIpReplacement(String args) {
        String type = RAND_IP_RANDOM;
        int count = 1;

        if (!args.isEmpty()) {
            String[] parts = args.split(",");
            String firstPart = parts[0].trim();
            if (!firstPart.isEmpty()) {
                // 判断第一个参数是数量还是类型
                try {
                    count = Integer.parseInt(firstPart);
                    type = RAND_IP_RANDOM;
                } catch (NumberFormatException e) {
                    type = firstPart.toLowerCase();
                }
            }
            if (parts.length >= 2) {
                try {
                    count = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    // 忽略无效的数量参数
                }
            }
        }

        // 限制数量范围，避免生成过多IP
        if (count < 1) count = 1;
        if (count > 20) count = 20;

        List<String> ips = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ips.add(generateIp(type));
        }
        return String.join(", ", ips);
    }

    private static String generateIp(String type) {
        switch (type) {
            case RAND_IP_PRIVATE:
                return generatePrivateIp();
            case RAND_IP_PUBLIC:
                return generatePublicIp();
            default:
                return generateRandomIp();
        }
    }

    /**
     * 生成内网IP地址（RFC 1918私有地址范围）。
     * 支持三种私有地址段：10.x.x.x / 172.16-31.x.x / 192.168.x.x
     */
    private static String generatePrivateIp() {
        int choice = random.nextInt(3);
        switch (choice) {
            case 0: // 10.0.0.0/8
                return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + (random.nextInt(254) + 1);
            case 1: // 172.16.0.0/12
                return "172." + (random.nextInt(16) + 16) + "." + random.nextInt(256) + "." + (random.nextInt(254) + 1);
            default: // 192.168.0.0/16
                return "192.168." + random.nextInt(256) + "." + (random.nextInt(254) + 1);
        }
    }

    /**
     * 生成外网（公网）IP地址，排除私有地址和特殊地址段。
     */
    private static String generatePublicIp() {
        String ip;
        do {
            ip = generateRandomIp();
        } while (isPrivateOrSpecial(ip));
        return ip;
    }

    /**
     * 生成完全随机的IP地址（1-223.x.x.1-254范围）。
     */
    private static String generateRandomIp() {
        return (random.nextInt(223) + 1) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + (random.nextInt(254) + 1);
    }

    /**
     * 判断IP是否属于私有或特殊地址段。
     */
    private static boolean isPrivateOrSpecial(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return true;
        try {
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            if (a == 10) return true;                            // 10.0.0.0/8
            if (a == 172 && b >= 16 && b <= 31) return true;    // 172.16.0.0/12
            if (a == 192 && b == 168) return true;               // 192.168.0.0/16
            if (a == 127) return true;                           // 127.0.0.0/8 loopback
            if (a == 169 && b == 254) return true;               // 169.254.0.0/16 link-local
            if (a == 100 && b >= 64 && b <= 127) return true;   // 100.64.0.0/10 shared address
            if (a == 192 && b == 0 && Integer.parseInt(parts[2]) == 2) return true;    // 192.0.2.0/24 TEST-NET-1
            if (a == 198 && b == 51 && Integer.parseInt(parts[2]) == 100) return true; // 198.51.100.0/24 TEST-NET-2
            if (a == 203 && b == 0 && Integer.parseInt(parts[2]) == 113) return true;  // 203.0.113.0/24 TEST-NET-3
        } catch (NumberFormatException e) {
            return true;
        }
        return false;
    }
}
