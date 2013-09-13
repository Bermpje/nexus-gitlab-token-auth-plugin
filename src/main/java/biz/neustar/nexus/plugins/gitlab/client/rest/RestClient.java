/*
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package biz.neustar.nexus.plugins.gitlab.client.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biz.neustar.nexus.plugins.gitlab.config.v1_0_0.Configuration;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.ApacheHttpClientState;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

/**
 *
 */
public class RestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);
	private static final Pattern ERROR_XML = Pattern.compile(".*<reason>(.*)</reason>.*<message>(.*)</message>.*", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private final URI serverURL;

	public RestClient(Configuration config) throws URISyntaxException {
		DefaultApacheHttpClientConfig clientConfig = new DefaultApacheHttpClientConfig();
		clientConfig.getProperties().put(ApacheHttpClientConfig.PROPERTY_HANDLE_COOKIES, Boolean.TRUE);
		//clientConfig.getProperties().put(ApacheHttpClientConfig.PROPERTY_PREEMPTIVE_AUTHENTICATION, Boolean.TRUE);
		clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, new Integer(config.getHttpTimeout()));
		clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, new Integer(config.getHttpTimeout()));
		clientConfig.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, new Integer(config.getHttpMaxConnections()));

		// /api/v3/user?private_token=<fill in>
		serverURL = new URI(config.getGitlabServerUrl()).resolve("/api/v3/");

		ApacheHttpClientState httpState = new ApacheHttpClientState();
		httpState.clearCredentials();
		if (StringUtils.isNotBlank(config.getHttpProxyHost()) && config.getHttpProxyPort() > 0) {
			clientConfig.getProperties().put(ApacheHttpClientConfig.PROPERTY_PROXY_URI, config.getHttpProxyHost() + ':' + config.getHttpProxyPort());

			if (config.getHttpProxyUsername() != null && config.getHttpProxyPassword() != null) {
				httpState.setProxyCredentials(null, config.getHttpProxyHost(), config.getHttpProxyPort(), config.getHttpProxyUsername(), config.getHttpProxyPassword());
			}
		}
		clientConfig.getProperties().put(ApacheHttpClientConfig.PROPERTY_HTTP_STATE, httpState);
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("Gitlab HTTP Client config");
		    LOGGER.debug(config.getGitlabServerUrl());
		    LOGGER.debug(serverURL.toString());
		    LOGGER.debug("PROPERTY_THREADPOOL_SIZE: {}", clientConfig.getProperty(ClientConfig.PROPERTY_THREADPOOL_SIZE));
		    LOGGER.debug("PROPERTY_READ_TIMEOUT: {}", clientConfig.getProperty(ClientConfig.PROPERTY_READ_TIMEOUT));
		    LOGGER.debug("PROPERTY_CONNECT_TIMEOUT: {}", clientConfig.getProperty(ClientConfig.PROPERTY_CONNECT_TIMEOUT));
		    LOGGER.debug("PROPERTY_PROXY_URI: {}", clientConfig.getProperty(ApacheHttpClientConfig.PROPERTY_PROXY_URI));

		}

		client = ApacheHttpClient.create(clientConfig);
	}

	/**
	 * Create new session token
	 *
	 * @param username
	 * @param password
	 * @return session token
	 * @throws RemoteException
	 *
	public String createSessionToken(String username, String password) throws RemoteException {
		if (LOG.isDebugEnabled()) LOG.debug("session creation attempt for '" + String.valueOf(username) + "'");

		WebResource r = client.resource(crowdServer.resolve("session"));

		SessionPost rBody = new SessionPost();
		rBody.username = username;
		rBody.password = password;
		try {
			SessionPostResponse response = r.accept(MediaType.APPLICATION_XML_TYPE).post(SessionPostResponse.class, rBody);

			if (LOG.isDebugEnabled()) LOG.debug(response.toString());

			LOG.info("session created for '" + String.valueOf(username) + "'");

			return response.token;
		} catch (UniformInterfaceException uie) {
			throw handleError(uie);
		}
	}
*/

	/**
	 * Retrieves the groups that the user is a nested member of
	 *
	 * @param username
	 * @return a set of roles (as strings)
	 * @throws RemoteException
	 *
	public Set<String> getNestedGroups(String username) throws RemoteException {
		if (LOG.isDebugEnabled()) LOG.debug("getNestedGroups(" + String.valueOf(username) + ")");

		WebResource r = client.resource(crowdServer.resolve("user/group/nested?username=" + username));

		try {
			GroupsResponse response = r.get(GroupsResponse.class);
			if (LOG.isDebugEnabled()) LOG.debug(response.toString());

			HashSet<String> result = new HashSet<String>();
			if (response.group != null) {
				for (GroupResponse group : response.group) {
					result.add(group.name);
				}
			}

			return result;

		} catch (UniformInterfaceException uie) {
			throw handleError(uie);
		}
	}
*/



	/**
	 * @param userid
	 * @return a <code>org.sonatype.security.usermanagement.User</code> from Gitlab by a userid
	 * @throws RemoteException
	 */
	public GitlabUser getUser(String userid, String token) throws RemoteException {
	    LOGGER.debug("getUser({}, xxxx)", String.valueOf(userid));

		WebResource r = client.resource(serverURL.resolve("user?private_token=" + token));
		//Map<String, String> response = new HashMap<String, String>();
		try {
		    // response = r.get(Map.class);
		    GitlabUser response = r.get(GitlabUser.class);
			LOGGER.debug(response.toString());
			return response;
		} catch (UniformInterfaceException uie) {
			throw handleError(uie);
		}

		//return convertUser(response);
	}


	/**
	 * Returns user list based on multiple criteria
	 * @param userId
	 * @param email
	 * @param filterGroups
	 * @param maxResults
	 * @return
	 * @throws RemoteException
	 * @throws UnsupportedEncodingException
	 *
	// XXX: seems Nexus 2.1.2 only search by userId
	// so we make the search in crowd on the userid OR email
	// A Nexus user will be able to make a lookup based on the email
	public Set<User> searchUsers(String userId, String email, Set<String> filterGroups, int maxResults) throws RemoteException, UnsupportedEncodingException {
		if (LOG.isDebugEnabled()) LOG.debug("searchUsers(" + String.valueOf(userId)
				+ "," + String.valueOf(email) + "," + String.valueOf(filterGroups) + "," + String.valueOf(maxResults) + ")");

		// find by user criteria 1st; then groups;
		if (StringUtils.isNotEmpty(userId) || StringUtils.isNotEmpty(email)) {
			StringBuilder restUri = new StringBuilder("search?entity-type=user&max-results=").append(maxResults).append("&restriction=");

			StringBuilder searchQuery = new StringBuilder("active = true");
			if (StringUtils.isNotEmpty(userId)) {
				searchQuery.append(" AND (name = \"").append(userId.trim()).append("\"")
					.append(" OR email = \"" + userId.trim() + "\")");
			}
			if (StringUtils.isNotEmpty(email)) {
				searchQuery.append(" AND email = \"").append(email.trim()).append("\"");
			}

			// URL encoding
			restUri.append(URLEncoder.encode(searchQuery.toString(), "UTF-8"));

			WebResource r = client.resource(crowdServer.resolve(restUri.toString()));
			HashSet<User> result = new HashSet<User>();
			try {
				SearchUserGetResponse response = r.get(SearchUserGetResponse.class);

				if (response.user != null) {
					for (UserResponse user : response.user) {
						result.add(getUser(user.name));
					}
				}

			} catch (UniformInterfaceException uie) {
				throw handleError(uie);
			}

			// filter groups
			if (filterGroups != null && !filterGroups.isEmpty()) {
				for (User user : result) {
					Set<String> userGroups = getNestedGroups(user.getUserId());
					boolean remove = true;
					for (String filterGoup : filterGroups) {
						if (userGroups.contains(filterGoup)) {
							remove = false;
							break;
						}
					}
					if (remove) {
						result.remove(user);
					}
				}
			}

			return result;
		}

		// find by groups only
		else {
			if (filterGroups != null && !filterGroups.isEmpty()) {
				Set<User> result = new HashSet<User>();

				for (String filterGroup : filterGroups) {
					WebResource r = client.resource(crowdServer.resolve("group/user/nested?groupname=" + filterGroup));

					try {
						SearchUserGetResponse response = r.get(SearchUserGetResponse.class);
						if (response.user != null) {
							for (UserResponse user : response.user) {
								// filter out inactive users
								if (user.active) {
									result.add(getUser(user.name));
								}
							}
						}
					} catch (UniformInterfaceException uie) {
						throw handleError(uie);
					}


					if (result.size() > maxResults) {
						break;
					}
				}

				return result;
			}
		}

		return Collections.emptySet();
	}

*/


	/**
	 *
	 * @return all the crowd groups
	 * @throws RemoteException
	 *
	public Set<Role> getAllGroups() throws RemoteException {
		LOG.debug("getAllGroups()");

		WebResource r = client.resource(crowdServer.resolve("search?entity-type=group"));

		try {
			GroupsResponse response = r.get(GroupsResponse.class);
			if (LOG.isDebugEnabled()) LOG.debug(response.toString());

			HashSet<Role> result = new HashSet<Role>();
			if (response.group != null) {
				for (GroupResponse group : response.group) {
					result.add(new Role(group.name, group.name, "", "", true, null, null));
				}
			}

			return result;
		} catch (UniformInterfaceException uie) {
			throw handleError(uie);
		}
	}*/

/*
	protected static User convertUser(Map<String, String> in) {
		User user = new DefaultUser();
		String name = nullToEmpty(in.get("name"));

		String firstName = name;
        String lastName = "";
		int split = name.indexOf(',');

		if (split > 0) {
		    lastName = name.substring(0, split).trim();
            firstName = name.substring(split + 1).trim();
        } else {
            split = name.indexOf(' ');
            if (split > 0) {
                firstName = name.substring(0, split).trim();
                lastName = name.substring(split + 1).trim();
            }
		}

		user.setUserId(in.get("username"));
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmailAddress(nullToEmpty(in.get("email")));
		String state = nullToEmpty(in.get("state"));
		boolean active = (state.equalsIgnoreCase("active"));
		user.setStatus(active ? UserStatus.active : UserStatus.disabled);
		return user;
	}*/


	public static String nullToEmpty(String s) {
	    return s == null ? "" : s;
	}
/*
	private Role convertGroup(GroupResponse in) {
        Role role = new Role();
        role.setRoleId(in.name);
        role.setName(in.name);
        role.setDescription(in.description);
        role.setReadOnly(true);
		return role;
	}
*/
	private RemoteException handleError(UniformInterfaceException uie) {
		ClientResponse response = uie.getResponse();
		String errorXml = response.getEntity(String.class);
		if (errorXml != null) {
			Matcher matcher = ERROR_XML.matcher(errorXml);
			if (matcher.matches()) {
				return new RemoteException(matcher.group(1) + ": " + matcher.group(2));
			}
		}

		return new RemoteException("Error in a Crowd REST call", uie);
	}
}
