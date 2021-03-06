package de.bitdroid.flooding.auth;


import android.accounts.Account;

import com.google.android.gms.auth.GoogleAuthException;

import org.roboguice.shaded.goole.common.base.Optional;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.client.UrlConnectionClient;


/**
 * A retrofit {@link UrlConnectionClient} which signs outgoing
 * requests with access token.
 */
public class AuthClient extends UrlConnectionClient {

	private final LoginManager loginManager;

	@Inject
	AuthClient(LoginManager loginManager) {
		this.loginManager = loginManager;
	}


	@Override
	public Response execute(Request request) throws IOException {
		try {
			List<Header> headers = new LinkedList<Header>(request.getHeaders());

			// if logged in add auth header
			Optional<Account> account = loginManager.getAccount();
			if (account.isPresent()) {
				String token = loginManager.getToken(account.get());
				Header authHeader = new Header("Authorization", "Bearer " + token);
				headers.add(authHeader);
			}

			Request signedRequest = new Request(
					request.getMethod(),
					request.getUrl(),
					headers,
					request.getBody());

			return super.execute(signedRequest);

		} catch (GoogleAuthException gae) {
			throw new IOException(gae);
		}
	}

}
