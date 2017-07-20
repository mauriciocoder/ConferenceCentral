package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.factory;
import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.devrel.training.conference.domain.Announcement;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = {
        Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @param profileForm
     *            A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException
     *             when the User object is null.
     */

    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that
    // conforms to the fields defined in ProfileForm

    // TODO 1 Pass the ProfileForm parameter
    // TODO 2 Pass the User parameter
    public Profile saveProfile(User user, ProfileForm form) throws UnauthorizedException {

        String userId = null;
        String mainEmail = null;
        String displayName = "Your name will go here";
        TeeShirtSize teeShirtSize = TeeShirtSize.NOT_SPECIFIED;

        // TODO 2
        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) {
            throw new UnauthorizedException("User is not logged in");
        }

        // TODO 1
        // Set the teeShirtSize to the value sent by the ProfileForm, if sent
        // otherwise leave it as the default value
        TeeShirtSize formTeeShirtSize = form.getTeeShirtSize();
        if (formTeeShirtSize != null) {
            teeShirtSize = formTeeShirtSize;
        }

        // TODO 1
        // Set the displayName to the value sent by the ProfileForm, if sent
        // otherwise set it to null
        displayName = form.getDisplayName();
        
        // TODO 2
        // Get the userId and mainEmail
        userId = user.getUserId();
        mainEmail = user.getEmail();
        
        // TODO 2
        // If the displayName is null, set it to default value based on the user's email
        // by calling extractDefaultDisplayNameFromEmail(...)
        if (displayName == null) {
            displayName = extractDefaultDisplayNameFromEmail(mainEmail);
        }
    
        Profile currentProfile = getProfile(user);
        Profile profile = null;
        if (currentProfile != null) {
            profile = currentProfile;
            profile.update(displayName, teeShirtSize);
        } else {
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
            profile.save();
        }
        
        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException
     *             when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // load the Profile Entity
        String userId = user.getUserId(); // TODO
        Key<Profile> key = Key.create(Profile.class, userId); // TODO
        Profile profile = (Profile) ofy().load().key(key).now(); // TODO load the Profile entity
        return profile;
    }
    
    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO (Lesson 4)
        // Get the userId of the logged in User
        final String userId = user.getUserId();

        // TODO (Lesson 4)
        // Get the key for the User's Profile
        Key<Profile> profileKey = Key.create(Profile.class, userId);

        // TODO (Lesson 4)
        // Allocate a key for the conference -- let App Engine allocate the ID
        // Don't forget to include the parent Profile in the allocated ID
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        // TODO (Lesson 4)
        // Get the Conference Id from the Key
        final long conferenceId = conferenceKey.getId();


        // TODO (Lesson 4)
        // Get the existing Profile entity for the current user if there is one
        // Otherwise create a new Profile entity with default values
        final Profile profile = getProfile(user);

        Conference conference = ofy().transact(new Work<Conference>() {
           public Conference run() {
               // TODO (Lesson 4)
               // Create a new Conference Entity, specifying the user's Profile entity
               // as the parent of the conference
               Conference conference = new Conference(conferenceId, userId, conferenceForm);
               // TODO (Lesson 4)
               // Save Conference and Profile Entities
               conference.save();
               profile.save();

               // Add to queue
               Queue queue = QueueFactory.getDefaultQueue();
               queue.add(ofy().getTransaction(),
                       TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                        .param("email", profile.getMainEmail())
                        .param("conferenceInfo", conference.toString()));

               return conference;
           }
        });
        return conference;
    }
    
    @ApiMethod(name = "queryConferences", path = "queryConferences", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferences(ConferenceQueryForm form) {
        //Query q = ofy().load().type(Conference.class).order("name");
        //return q.list();
        return form.getQuery().list();
    }
    
    @ApiMethod(name = "queryConferencesCreated", path = "queryConferencesCreated", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferencesCreated(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Get the userId of the logged in User
        String userId = user.getUserId();
        
        Query q = ofy().load().type(Conference.class).ancestor(Key.create(Profile.class, userId)).order("name");
        return q.list();
    }
    
    @ApiMethod(name = "queryConferencesByFilter", path = "queryConferencesByFilter", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferencesByFilter(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Get the userId of the logged in User
        String userId = user.getUserId();
        
        Query q = ofy().load().type(Conference.class).order("name");
        q = q.filter("topics = ", "Medical Innovations");
        q = q.filter("city = ", "London");
        return q.list();
    }

    @ApiMethod(name = "queryConferencesPlayground", path = "queryConferencesPlayground", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferencesPlayground(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Get the userId of the logged in User
        String userId = user.getUserId();

        Query q = ofy().load().type(Conference.class).order("name");
        q = q.filter("month = ", "6");
        q = q.filter("maxAttendees > ", "10");
        q = q.filter("city = ", "London");
        return q.list();
    }

    @ApiMethod(name = "queryConferencesPlayground2", path = "queryConferencesPlayground2", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferencesPlayground2(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Get the userId of the logged in User
        String userId = user.getUserId();

        Query q = ofy().load().type(Conference.class).order("name");
        q = q.filter("month = ", "6");
        q = q.filter("city = ", "London");
        q = q.filter("topics = ", "Medical Innovations");
        return q.list();
    }

    @ApiMethod(name = "queryConferencesPlayground3", path = "queryConferencesPlayground3", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferencesPlayground3(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Get the userId of the logged in User
        String userId = user.getUserId();

        Query q = ofy().load().type(Conference.class).order("city");
        q = q.filter("topics = ", "Medical Innovations");
        return q.list();
    }

    /**
     * Just a wrapper for Boolean.
     * We need this wrapped Boolean because endpoints functions must return
     * an object instance, they can't return a Type class such as
     * String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Returns a Conference object with the given conferenceId.
     *
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return a Conference object with the given conferenceId.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "getConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.GET
    )
    public Conference getConference(
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException(conferenceKey);
        }
        return conference;
    }

    /**
     * Register to attend the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
    */
    @ApiMethod(name = "registerForConference", path = "conference/{websafeConferenceKey}/registration", httpMethod = HttpMethod.POST)
    public WrappedBoolean registerForConference(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = user.getUserId();

        // TODO
        // Start transaction
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {
                    // TODO
                    // Get the conference key -- you can get it from websafeConferenceKey
                    // Will throw ForbiddenException if the key cannot be created
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                    // TODO
                    // Get the Conference entity from the datastore
                    Conference conference = (Conference) ofy().load().key(conferenceKey).now();

                    // 404 when there is no Conference with the given conferenceId.
                    if (conference == null) {
                        return new WrappedBoolean (false,"No Conference found with key: " + websafeConferenceKey);
                    }

                    // TODO
                    // Get the user's Profile entity
                    Profile profile = getProfile(user);

                    // Has the user already registered to attend this conference?
                    if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                        return new WrappedBoolean (false, "Already registered");
                    } else if (conference.getSeatsAvailable() <= 0) {
                        return new WrappedBoolean (false, "No seats available");
                    } else {
                        // All looks good, go ahead and book the seat
                        // TODO
                        // Add the websafeConferenceKey to the profile's
                        // conferencesToAttend property
                        profile.addToConferenceKeysToAttend(websafeConferenceKey);

                        // TODO
                        // Decrease the conference's seatsAvailable
                        // You can use the bookSeats() method on Conference
                        conference.bookSeats(1);

                        // TODO
                        // Save the Conference and Profile entities
                        conference.save();
                        profile.save();

                        // We are booked!
                        return new WrappedBoolean(true, "Registration successful");
                    }
                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }
        });

        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException (Key.create(websafeConferenceKey));
            }
            else if (result.getReason() == "Already registered") {
                throw new ConflictException("You have already registered");
            }
            else if (result.getReason() == "No seats available") {
                throw new ConflictException("There are no seats available");
            }
            else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }

    /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getConferencesToAttend",
            path = "getConferencesToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // TODO
        // Get the Profile entity for the user
        Profile profile = getProfile(user);
        if (profile == null) {
            throw new NotFoundException(null);
        }

        // TODO
        // Get the value of the profile's conferenceKeysToAttend property
        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend();

        // TODO
        // Iterate over keyStringsToAttend,
        // and return a Collection of the
        // Conference entities that the user has registered to atend
        List<Key<Conference>> keys = new ArrayList<>();
        for (String k : keyStringsToAttend) {
            Key<Conference> key = Key.create(k);
            keys.add(key);
        }
        Collection<Conference> collectionsToAttend = (Collection<Conference>) ofy().load().keys(keys).values();
        return collectionsToAttend;
    }

    /**
     * Unregister from specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(name = "unregisterFromConference", path = "conference/{websafeConferenceKey}/unregistration", httpMethod = HttpMethod.POST)
    public WrappedBoolean unregisterFromConference(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Start transaction
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
               @Override
               public WrappedBoolean run() {
                   try {
                       // Get the Profile entity for the user
                       Profile profile = getProfile(user);
                       if (profile == null) {
                           throw new NotFoundException(null);
                       }

                       Key<Conference> key = Key.create(websafeConferenceKey);
                       if (!profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                           throw new NotFoundException(key);
                       }

                       profile.removeConferenceToAttend(websafeConferenceKey);
                       Conference conference = (Conference) ofy().load().key(key).now();
                       conference.giveBackSeats(1);

                       profile.save();
                       conference.save();

                       // We are booked!
                       return new WrappedBoolean(true, "Registration successful");
                   } catch (Exception e) {
                       return new WrappedBoolean(false, "Unknown exception");
                   }
               }
           }
        );
        return result;
    }

    /**
     * Returns a collection of Announcements
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return an Announcement
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(name = "getAnnouncement", path = "getAnnouncement", httpMethod = HttpMethod.GET)
    public Announcement getAnnouncement(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Get the Profile entity for the user
        Profile profile = getProfile(user);
        if (profile == null) {
            throw new NotFoundException(null);
        }

        MemcacheService service = MemcacheServiceFactory.getMemcacheService();
        Announcement a = new Announcement((String) service.get(Constants.MEMCACHE_ANNOUNCEMENTS_KEY));
        return a;
    }
}
