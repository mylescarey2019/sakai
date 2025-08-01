/**
 * Copyright (c) 2008-2012 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.profile2.tool.entityprovider;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityURLRedirect;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Redirectable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Sampleable;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.entitybroker.util.TemplateParseUtil;
import org.sakaiproject.profile2.logic.ProfileImageLogic;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.model.MimeTypeByteArray;
import org.sakaiproject.profile2.model.ProfileImage;
import org.sakaiproject.profile2.model.UserProfile;
import org.sakaiproject.profile2.util.Messages;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;

/**
 * This is the entity provider for a user's profile.
 * 
 * @author Steve Swinsburg (s.swinsburg@lancaster.ac.uk)
 *
 */
@Slf4j
public class ProfileEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Outputable, Resolvable, Sampleable, Describeable, Redirectable, ActionsExecutable, RequestAware {

	public final static String ENTITY_PREFIX = "profile";
	
	@Override
	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}
		
	@Override
	public boolean entityExists(String eid) {
		return true;
	}

	@Override
	public Object getSampleEntity() {
		return new UserProfile();
	}
	
	@Override
	public Object getEntity(EntityReference ref) {
	
		//convert input to uuid
		String uuid = sakaiProxy.ensureUuid(ref.getId());
		if(StringUtils.isBlank(uuid)) {
			throw new EntityNotFoundException("Invalid user.", ref.getId());
		}
		
		//check for siteId in the request
		String siteId = requestGetter.getRequest().getParameter("siteId");
		
		//get the full profile for the user, takes care of privacy checks against the current user
		UserProfile userProfile = profileLogic.getUserProfile(uuid, siteId);
		if(userProfile == null) {
			throw new EntityNotFoundException("Profile could not be retrieved for " + ref.getId(), ref.getReference());
		}
		return userProfile;
	}

	@EntityCustomAction(action="image",viewKey=EntityView.VIEW_SHOW)
	public Object getProfileImage(OutputStream out, EntityView view, Map<String,Object> params, EntityReference ref) {
		
		final String id = ref.getId();

        // A role swapped user is not "real" so just use the blank image
        boolean wantsBlank = id.equals(ProfileConstants.BLANK) || sakaiProxy.isUserRoleSwapped();

        String uuid = "";
        String currentUserId = sakaiProxy.getCurrentUserId();

        if(!wantsBlank) {
		    //convert input to uuid
		    uuid = sakaiProxy.ensureUuid(ref.getId());
            if(StringUtils.isBlank(uuid)) {
                throw new EntityNotFoundException("Invalid user.", ref.getId());
            }
        }
		
		ProfileImage image = null;
		final boolean wantsThumbnail = StringUtils.equals("thumb", view.getPathSegment(3)) ? true : false;
		
		boolean wantsAvatar = false;
		if(!wantsThumbnail) {
			wantsAvatar = StringUtils.equals("avatar", view.getPathSegment(3)) ? true : false;
		}
		
		final boolean wantsOfficial = StringUtils.equals("official", view.getPathSegment(3)) ? true : false;

		if(log.isDebugEnabled()) {
			log.debug("wantsThumbnail:" + wantsThumbnail);
			log.debug("wantsAvatar:" + wantsAvatar);
			log.debug("wantsOfficial:" + wantsOfficial);
			log.debug("wantsBlank:" + wantsBlank);
		}
		
		//optional siteid
		final String siteId = (String)params.get("siteId");
		if(StringUtils.isNotBlank(siteId) && !sakaiProxy.checkForSite(siteId)){
			throw new EntityNotFoundException("Invalid siteId: " + siteId, ref.getReference());
		}

		// First of all, check if the current user is admin. If current user is admin, show all the pictures always
		if (sakaiProxy.isSuperUser()) {
			wantsBlank = false;
		} else if (StringUtils.isBlank(siteId)) {
			// No site id is specified, checking if both users have any site in common
			if (!sakaiProxy.areUsersMembersOfSameSite(currentUserId, uuid)) {
				// No sites in common, so serving a blank image
				wantsBlank = true;
			}
		} else {
			// Site id is specified, checking if both users are members of that site
			if (!sakaiProxy.isUserMemberOfSite(currentUserId, siteId)) {
				// Current user is not a member of the specified site, so serving a blank image
				wantsBlank = true;
			}
			if (!sakaiProxy.isUserMemberOfSite(uuid, siteId)) {
				// Requested user is not a member of the specified site, so serving a blank image
				wantsBlank = true;
			}
		}

        if(wantsBlank) {
            image = imageLogic.getBlankProfileImage();
        } else {
		    //get thumb or avatar if requested - or fallback
            if(wantsThumbnail) {
                image = imageLogic.getProfileImage(uuid, ProfileConstants.PROFILE_IMAGE_THUMBNAIL, siteId);
            } 
            if(!wantsThumbnail && wantsAvatar) {
                image = imageLogic.getProfileImage(uuid, ProfileConstants.PROFILE_IMAGE_AVATAR, siteId);
            }
            if(!wantsThumbnail && !wantsAvatar) {
                image = imageLogic.getProfileImage(uuid, ProfileConstants.PROFILE_IMAGE_MAIN, siteId);
            }
            if(wantsOfficial) {
			    image = imageLogic.getOfficialProfileImage(uuid, siteId);
		    }
        }
		
		if(image == null) {
			throw new EntityNotFoundException("No profile image for " + id, ref.getReference());
		}

		if (!StringUtils.equals(currentUserId, uuid)) {
			sakaiProxy.postEvent(ProfileConstants.EVENT_IMAGE_REQUEST, "/profile/" + currentUserId + "/imagerequest/" + uuid, false);
		}

		//check for binary
		final byte[] bytes = image.getBinary();
		if(bytes != null && bytes.length > 0) {
			try {
				out.write(bytes);
				return new ActionReturn("UTF-8", image.getMimeType(), out);
			} catch (IOException e) {
				throw new EntityException("Error retrieving profile image for " + id + " : " + e.getMessage(), ref.getReference());
			}
		}
		
		final String url = image.getUrl();
		if(StringUtils.isNotBlank(url)) {
			try {
				HttpServletResponse res = requestGetter.getResponse();
				res.addHeader("Cache-Control","no-store");
				res.sendRedirect(url);
			} catch (IOException e) {
				throw new EntityException("Error redirecting to external image for " + id + " : " + e.getMessage(), ref.getReference());
			}
		}
		
		return null;
	}
	
	@EntityURLRedirect("/{prefix}/{id}/account")
	public String redirectUserAccount(Map<String,String> vars) {
		return "user/" + vars.get("id") + vars.get(TemplateParseUtil.DOT_EXTENSION);
	}

	@EntityCustomAction(action="pronunciation",viewKey=EntityView.VIEW_SHOW)
	public Object getNamePronunciation(OutputStream out, EntityView view, Map<String,Object> params, EntityReference ref) {
		if (!sakaiProxy.isLoggedIn()) {
			throw new SecurityException("You must be logged in to get the name pronunciation of the student.");
		}
		String uuid = sakaiProxy.ensureUuid(ref.getId());
		if(StringUtils.isBlank(uuid)) {
			throw new EntityNotFoundException("Invalid user.", ref.getId());
		}
		
		MimeTypeByteArray mtba = profileLogic.getUserNamePronunciation(uuid);
		if(mtba != null && mtba.getBytes() != null) {
			try {
				HttpServletResponse response = requestGetter.getResponse();
				HttpServletRequest request = requestGetter.getRequest();
				response.setHeader("Cache-Control", "no-store");
				response.setContentType(mtba.getMimeType());

				// Are we processing a Range request
				if (request.getHeader(HttpHeaders.RANGE) == null) {
					// Not a Range request
					byte[] bytes = mtba.getBytes();
					response.setContentLengthLong(bytes.length);
					out.write(bytes);
					return new ActionReturn(Formats.UTF_8, mtba.getMimeType() , out);
 				} else {
					// A Range request - we use springs HttpRange class
					Resource resource = new ByteArrayResource(mtba.getBytes());
					response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
					response.setContentLengthLong(resource.contentLength());
					response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
					try {
						ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
						ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);

						List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
						ResourceRegionHttpMessageConverter messageConverter = new ResourceRegionHttpMessageConverter();

						if (httpRanges.size() == 1) {
							ResourceRegion resourceRegion = httpRanges.get(0).toResourceRegion(resource);
							messageConverter.write(resourceRegion, MediaType.parseMediaType(mtba.getMimeType()), outputMessage);
						} else {
							messageConverter.write(HttpRange.toResourceRegions(httpRanges, resource), MediaType.parseMediaType(mtba.getMimeType()), outputMessage);
						}
					} catch (IllegalArgumentException iae) {
						response.setHeader("Content-Range", "bytes */" + resource.contentLength());
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						log.warn("Name pronunciation request failed to send the requested range for {}, {}", ref.getReference(), iae.getMessage());
					}
				}
			} catch (Exception e) {
				throw new EntityException("Name pronunciation request failed, " + e.getMessage(), ref.getReference());
			}
		}
		return null;
	}
	
	@Override
	public String[] getHandledOutputFormats() {
		return new String[] {Formats.HTML, Formats.XML, Formats.JSON};
	}
	
	@Setter
	private RequestGetter requestGetter;
	
	@Setter
	private SakaiProxy sakaiProxy;
	
	@Setter
	private ProfileLogic profileLogic;
	
	@Setter	
	private ProfileImageLogic imageLogic;
}
