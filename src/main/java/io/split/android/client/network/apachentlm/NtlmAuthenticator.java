package io.split.android.client.network.apachentlm;


import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import io.split.android.client.utils.Logger;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

//import jcifs.ntlmssp.NtlmFlags;
//import jcifs.ntlmssp.Type1Message;
//import jcifs.ntlmssp.Type2Message;
//import jcifs.ntlmssp.Type3Message;
//import jcifs.util.Base64;

public class NtlmAuthenticator implements Authenticator {
    final static String EMPTY_STRING = "";
    private static final String CLASS_TAG = "NtlmAuthenticator";
    private static final String AUTHENTICATE_HEADERS = "Proxy-Authenticate";
    private static final String HEADER_NEGOTIATE = "Negotiate";
    private static final String HEADER_NTLM = "NTLM";
    private static final String HEADER_NTLM_CHALENGE_PREFIX = "NTLM ";
    private static final int HEADER_NTLM_CHALENGE_START = 5;
    private static final String HEADER_AUTHORIZATION = "Authorization";

    NTLMEngine mNltmEngine = new NTLMEngineImpl();

    private String mUser;
    private String mPassword;
    private String mDomain;
    private String mWorkstation;

    private int mMaxRequestLoop;
    private HttpUrl mUrl;

    public NtlmAuthenticator(@NonNull String login, @NonNull String password) {
        this(login, password, EMPTY_STRING, EMPTY_STRING);
    }

    public NtlmAuthenticator(@NonNull String login, @NonNull String password, @NonNull String domain, @NonNull String workstation) {
        mUser = login;
        mPassword = password;
        mDomain = domain;
        mWorkstation = workstation;
        mMaxRequestLoop = 0;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (mUrl == null || response.request().url() != mUrl) {
            mMaxRequestLoop = 0;
            mUrl = response.request().url();
        }

        mMaxRequestLoop++;

        if (mMaxRequestLoop >= 4) {
            throw new IOException(String.format("Challenge request count too big (%s)", mMaxRequestLoop));
        }

        List<String> authHeaders = response.headers(AUTHENTICATE_HEADERS);

        if (authHeaders != null) {
            boolean isNegociateHeader = false;
            boolean isNtlm = false;
            String ntlmChallenge = null;

            for (String authHeader : authHeaders) {
                isNegociateHeader = authHeader.equalsIgnoreCase(HEADER_NEGOTIATE);
                isNtlm = authHeader.equalsIgnoreCase(HEADER_NTLM);
                if (authHeader.startsWith(HEADER_NTLM_CHALENGE_PREFIX)) {
                    ntlmChallenge = authHeader.substring(HEADER_NTLM_CHALENGE_START);
                }
            }

            if (isNegociateHeader && isNtlm) {
                String type1Msg = generateType1Msg(mDomain, mWorkstation);
                String header = HEADER_NTLM_CHALENGE_PREFIX + type1Msg;

                return response.request().newBuilder().header(HEADER_AUTHORIZATION, header).build();
            } else if (ntlmChallenge != null) {
                String type3Msg = generateType3Msg(mUser, mPassword, mDomain, mWorkstation, ntlmChallenge);
                String ntlmHeader = HEADER_NTLM + " " + type3Msg;

                return response.request().newBuilder().header(HEADER_AUTHORIZATION, ntlmHeader).build();
            }
        }

        if (responseCount(response) <= 3) {
            String credential = Credentials.basic(mUser, mPassword);

            return response.request().newBuilder().header(HEADER_AUTHORIZATION, credential).build();
        }

        return null;
    }

    private String generateType1Header(@NonNull String domain, @NonNull String workstation) {
        return HEADER_NTLM_CHALENGE_PREFIX + generateType1Msg(mDomain, mWorkstation);
    }

    private String generateType1Msg(@NonNull String domain, @NonNull String workstation) {
        try {
            return mNltmEngine.generateType1Msg(domain, workstation);
        } catch (NTLMEngineException e) {
            Logger.e("An error has ocurred while generation Ntlm msg 1: " + e.getLocalizedMessage());
        }
        return EMPTY_STRING;
    }

    private String generateType3Msg(final String login, final String password, final String domain, final String workstation, final String challenge) {
        try {
            return mNltmEngine.generateType3Msg(login, password.toCharArray(), domain, workstation, challenge);
        } catch (NTLMEngineException e) {
            Logger.e("An error has ocurred while generation Ntlm msg 3: " + e.getLocalizedMessage());
        }
        return EMPTY_STRING;
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            if (!response.isRedirect())
                result++;
        }

        return result;
    }

}

