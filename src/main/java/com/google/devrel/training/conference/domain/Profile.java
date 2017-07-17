package com.google.devrel.training.conference.domain;

import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import static com.google.devrel.training.conference.service.OfyService.ofy;


// TODO indicate that this class is an Entity
@Entity
public class Profile {
	String displayName;
	String mainEmail;
	TeeShirtSize teeShirtSize;

	// TODO indicate that the userId is to be used in the Entity's key
	@Id
	String userId;
    
    /**
     * Public constructor for Profile.
     * @param userId The user id, obtained from the email
     * @param displayName Any string user wants us to display him/her on this system.
     * @param mainEmail User's main e-mail address.
     * @param teeShirtSize The User's tee shirt size
     * 
     */
    public Profile (String userId, String displayName, String mainEmail, TeeShirtSize teeShirtSize) {
    	this.userId = userId;
    	this.displayName = displayName;
    	this.mainEmail = mainEmail;
    	this.teeShirtSize = teeShirtSize;
    }
    
	public String getDisplayName() {
		return displayName;
	}

	public String getMainEmail() {
		return mainEmail;
	}

	public TeeShirtSize getTeeShirtSize() {
		return teeShirtSize;
	}

	public String getUserId() {
		return userId;
	}

	/**
     * Just making the default constructor private.
     */
    private Profile() {}
	
	public void save() {
		// TODO 3 (In Lesson 3)
		// Save the Profile entity in the datastore
		ofy().save().entity(this).now();
	}
	
	public void update(String displayName, TeeShirtSize teeShirtSize) {
    	if (!(displayName.equals(this.displayName) && teeShirtSize.equals(teeShirtSize))) {
    		this.displayName = displayName;
    		this.teeShirtSize = teeShirtSize;
    		save();
			}
	}
}
