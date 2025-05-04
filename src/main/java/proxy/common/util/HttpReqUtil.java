package proxy.common.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import proxy.common.config.KafkaRestConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * http request utility 클래스
 */
@Slf4j
public class HttpReqUtil {

    @Autowired
    private KafkaRestConfig kafkaRestConfig;

    private HttpRequest httpRequest = new HttpRequest(this);

    private URL url = null;
    private HttpURLConnection connection;

    public enum HTTP_METHOD {
        POST,
        GET,
        PUT,
        DELETE,
        PATCH;
    }

    /**
     * request url 설정
     * @param url
     * @throws IOException
     */
    private HttpReqUtil(String url) throws IOException {
        // url 시작점에 http 추가
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        // url 지정
        this.url = new URL(url);

        // 보안 설정이 적용된 url인 경우
        if (url.startsWith("https://")) {
            // ssl 인증 무시 설정이 되어있는 경우
            if (this.kafkaRestConfig.isIgnoreSslValidation()) {
                try {
                    ignoreSsl(); // ssl 인증 무시
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    log.error("", e);
                }
            }
            this.connection = (HttpsURLConnection) this.url.openConnection(); // http 연결
        } else {
            this.connection = (HttpURLConnection) this.url.openConnection(); // http 연결
        }
    }

    /**
     * url을 입력받아 HttpRequest 객체 생성
     * @param url
     * @return
     * @throws IOException
     */
    public static HttpRequest make(String url) throws IOException {
        HttpReqUtil httpReqUtils = new HttpReqUtil(url);

        return httpReqUtils.httpRequest;
    }

    /**
     * http 요청 처리 클래스
     */
    @AllArgsConstructor
    public class HttpRequest {
        private HttpReqUtil httpReqUtils;

        /**
         * Http method 설정
         * @param method
         * @return
         * @throws ProtocolException
         */
        public HttpRequest setMethod(HTTP_METHOD method) throws ProtocolException {
            this.httpReqUtils.connection.setRequestMethod(method.name());
            return this;
        }

        /**
         * Http header 추가
         * @param key
         * @param value
         * @return
         */
        public HttpRequest addHeader(String key, String value) {
            this.httpReqUtils.connection.setRequestProperty(key, value);
            return this;
        }

        /**
         * username, password 중 하나라도 주어지지 않은 경우, 설정하지 않음.
         * @param username
         * @param password
         * @return
         */
        public HttpRequest applyBasicAuth(String username, String password) {
            StringBuilder sb = new StringBuilder("");
            
            if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                byte[] bytes = sb.append(username).append(":").append(password).toString().getBytes(StandardCharsets.UTF_8);
                String base64EncodedStr = Base64.getEncoder().encodeToString(bytes);
                this.httpReqUtils.connection.setRequestProperty("Authorization", "Basic " + base64EncodedStr);
            }
            return this;
        }

        /**
         * 서버로 요청 전달
         * @return
         * @throws
         */
        public HttpResponse send() {
            return new HttpResponse(this.httpReqUtils);
        }

        /**
         * body가 있을 경우, body를 추가하여 요청
         * @param body
         * @return
         * @throws IOException
         */
        public HttpResponse sendBody(String body) throws IOException {
            this.httpReqUtils.connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(this.httpReqUtils.connection.getOutputStream());

            String payload = null;
            if (body != null) payload = body;
            else payload = StringUtil.EMPTY;
            wr.writeBytes(payload);
            wr.flush();
            wr.close();

            return new HttpResponse(this.httpReqUtils);
        }
    }

    /**
     * http 응답 처리 클래스
     */
    @AllArgsConstructor
    public class HttpResponse {
        private HttpReqUtil httpReqUtils;

        /**
         * 응답의 http-status 값 반환
         * @return
         * @throws IOException
         */
        public int getStatusCode() throws IOException {
            return this.httpReqUtils.connection.getResponseCode();
        }

        /**
         * http 요청이 정상처리 되었을 때, http-status 값이 200 이상이고 300 미만 일 때, http-response 값 가져옴.
         * @return
         * @throws IOException
         */
        public String getResponse() throws IOException {
            // 응답 상태코드 확인
        	var conn = this.httpReqUtils.connection;
            var resCode = conn.getResponseCode();
            if (200 <= resCode || 300 > resCode) {
            	InputStream response = null;
            	try {
            		response = conn.getInputStream();
            	} catch (Exception e) {
            		// response code에 따라 getInputStream에서 오류가 발생될 수 있음.
            		response = conn.getErrorStream();
            	}
            	
                return StringUtil.from(response);
            } else {
                return "";
            }
        }

        /**
         * http 요청이 정상처리 되지 않았을 경우, err-response 값 읽어옴.
         * @return
         * @throws IOException
         */
        public String getErrResponse() throws IOException {
            String output = null;
            if (null != this.httpReqUtils.connection.getErrorStream()) {
                output = StringUtil.from(this.httpReqUtils.connection.getErrorStream());
            } else {
                output = StringUtil.EMPTY;
            }
            return output;
        }

        /**
         * http-status에 상관없이 response-body 값 읽어옴.
         * @return
         * @throws IOException
         */
        public String getResponseOrErr() throws IOException {
            String output = this.getResponse();
            if (0 == output.length()) {
                output = this.getErrResponse();
            }
            return output;
        }
    }

    /**
     * 입력된 url이 https일 때, ssl-verify를 하지 않도록 설정
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static void ignoreSsl() throws NoSuchAlgorithmException, KeyManagementException {
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session){
                return true;
            }
        };
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    /**
     * ssl-verify를 하지 않도록 설정
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static void trustAllHttpsCertificates() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    /**
     * 모든 ssl을 검사하지 않고 허용
     */
    static class miTM implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            return;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            return;
        }
    }
}
