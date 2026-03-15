package com.blog.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

public final class UrlSecurityUtil {

    private static final int MAX_PORT = 65535;

    private UrlSecurityUtil() {
    }

    public static void validatePublicHttpUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Only HTTP/HTTPS URLs are allowed");
            }

            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException("URL user info is not allowed");
            }

            int port = uri.getPort();
            if (port > MAX_PORT) {
                throw new IllegalArgumentException("Invalid URL port");
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Invalid URL host");
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(normalizedHost)
                    || normalizedHost.endsWith(".localhost")
                    || normalizedHost.endsWith(".local")
                    || normalizedHost.endsWith(".internal")) {
                throw new IllegalArgumentException("Localhost is not allowed");
            }

            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("Cannot resolve host");
            }

            for (InetAddress address : addresses) {
                if (isPrivateOrReserved(address)) {
                    throw new IllegalArgumentException("Private or local network addresses are not allowed");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    private static boolean isPrivateOrReserved(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            if (b0 == 10) {
                return true;
            }
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                return true;
            }
            if (b0 == 127) {
                return true;
            }
            if (b0 == 169 && b1 == 254) {
                return true;
            }
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) {
                return true;
            }
            if (b0 == 192 && b1 == 168) {
                return true;
            }
            if (b0 == 0) {
                return true;
            }
        }

        if (address instanceof Inet6Address inet6) {
            byte[] bytes = inet6.getAddress();
            int firstByte = bytes[0] & 0xFF;
            return (firstByte & 0xFE) == 0xFC;
        }

        return false;
    }
}
