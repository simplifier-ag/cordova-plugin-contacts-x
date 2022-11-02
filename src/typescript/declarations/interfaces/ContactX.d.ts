declare module 'cordova-plugin-contacts-x' {
  
  type ContactType = 'home' |'work' | 'mobile' | 'other';
  type AddressType = 'home' | 'work' | 'other' | 'custom';

  interface ContactXPhoneNumber {
    id?: string;

    /**
     * type of the phoneNumber
     */
    type: ContactType;

    /**
     * the phoneNumber itself
     */
    value: string;
  }

  interface ContactXEmail {
    id?: string;

    /**
     * type of the mail
     */
    type: ContactType;

    /**
     * the mail itself
     */
    value: string;
  }

  interface ContactXAdress {
	id?: number;

	/**
	 * type of address
	 */
	type?: AddressType;

	/**
	 * street name
	 */
	street?: string;

	/**
	 * city name
	 */
	city?: string;

	/**
	 * region code of country
	 */
	region?: string;

	/**
	 * postal code / zip code
	 */
	postCode?: string;

	/**
	 * full country name
	 */
	country?: string;

  }

  interface ContactX {
    id?: string;
    rawId?: string;

    /**
     * android only
     */
    displayName?: string;

    /**
     * first name (given name) of the contact
     */
    firstName?: string;

    /**
     * middle name of the contact
     */
    middleName?: string;

    /**
     * family name of the contact
     */
    familyName?: string;

    /**
     * organization name of the contact
     */
    organizationName?: string;

    /**
     * unformatted phone-numbers of the contact
     */
    phoneNumbers?: ContactXPhoneNumber[];

    /**
     * unformatted emails of the contact
     */
    emails?: ContactXEmail[];

	/**
	 * adresses of the contact
	 */
	addresses?: ContactXAdress[];

  }
}
