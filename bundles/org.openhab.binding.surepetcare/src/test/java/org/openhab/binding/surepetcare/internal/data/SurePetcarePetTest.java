/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.surepetcare.internal.data;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.openhab.binding.surepetcare.internal.SurePetcareConstants;
import org.openhab.binding.surepetcare.internal.dto.SurePetcarePet;

/**
 * The {@link SurePetcarePetTest} class implements unit test case for {@link SurePetcarePet}
 *
 * @author Rene Scherer - Initial contribution
 */
@NonNullByDefault
public class SurePetcarePetTest {

    // {
    // "id":34675,
    // "name":"Cat",
    // "gender":0,
    // "date_of_birth":"2017-08-01T00:00:00+00:00",
    // "weight":"3.5",
    // "comments":"Test Comment",
    // "household_id":87435,
    // "breed_id":382,
    // "photo_id":23412,
    // "species_id":1,
    // "tag_id":60456,
    // "version":"Mw==",
    // "created_at":"2019-09-02T09:27:17+00:00",
    // "updated_at":"2019-10-03T12:17:48+00:00",
    // "conditions":[
    // {
    // "id":18,
    // "version":"MA==",
    // "created_at":"2019-10-03T12:17:48+00:00",
    // "updated_at":"2019-10-03T12:17:48+00:00"
    // },
    // {
    // "id":17,
    // "version":"MA==",
    // "created_at":"2019-10-03T12:17:48+00:00",
    // "updated_at":"2019-10-03T12:17:48+00:00"
    // }
    // ],
    // "photo":{
    // "id":79293,
    // "location":"https:\/\/surehub.s3.amazonaws.com\/user-photos\/thm\/23412\/z70LUtqaHVhlsdfuyHKJH5HDysg5AR6GvQwdAZptCgeZU.jpg",
    // "uploading_user_id":52815,
    // "version":"MA==",
    // "created_at":"2019-09-02T09:31:07+00:00",
    // "updated_at":"2019-09-02T09:31:07+00:00"
    // },
    // "position":{
    // "tag_id":60456,
    // "device_id":318986,
    // "where":1,
    // "since":"2019-10-03T10:23:37+00:00"
    // },
    // "status":{
    // "activity":{
    // "tag_id":60456,
    // "device_id":318986,
    // "where":1,
    // "since":"2019-10-03T10:23:37+00:00"
    // }
    // }
    // }

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Test
    public void testJsonDeserialize() throws ParseException {
        String testReponse = "{\"id\":34675,\"name\":\"Cat\",\"gender\":0,\"date_of_birth\":\"2017-08-01T00:00:00+00:00\",\"weight\":\"3.5\",\"comments\":\"Test Comment\",\"household_id\":87435,\"breed_id\":382,\"photo_id\":23412,\"species_id\":1,\"tag_id\":60456,\"version\":\"Mw==\",\"created_at\":\"2019-09-02T09:27:17+00:00\",\"updated_at\":\"2019-10-03T12:17:48+00:00\",\"conditions\":[{\"id\":18,\"version\":\"MA==\",\"created_at\":\"2019-10-03T12:17:48+00:00\",\"updated_at\":\"2019-10-03T12:17:48+00:00\"},{\"id\":17,\"version\":\"MA==\",\"created_at\":\"2019-10-03T12:17:48+00:00\",\"updated_at\":\"2019-10-03T12:17:48+00:00\"}],\"photo\":{\"id\":79293,\"location\":\"https:\\/\\/surehub.s3.amazonaws.com\\/user-photos\\/thm\\/23412\\/z70LUtqaHVhlsdfuyHKJH5HDysg5AR6GvQwdAZptCgeZU.jpg\",\"uploading_user_id\":52815,\"version\":\"MA==\",\"created_at\":\"2019-09-02T09:31:07+00:00\",\"updated_at\":\"2019-09-02T09:31:07+00:00\"},\"position\":{\"tag_id\":60456,\"device_id\":318986,\"where\":1,\"since\":\"2019-10-03T10:23:37+00:00\"},\"status\":{\"activity\":{\"tag_id\":60456,\"device_id\":318986,\"where\":1,\"since\":\"2019-10-03T10:23:37+00:00\"}}}";
        SurePetcarePet response = SurePetcareConstants.GSON.fromJson(testReponse, SurePetcarePet.class);

        assertEquals(Integer.valueOf(34675), response.id);
        assertEquals("Cat", response.name);
        assertEquals(Integer.valueOf(0), response.genderId);

        Date dobDate;
        synchronized (simpleDateFormat) {
            dobDate = simpleDateFormat.parse("2017-08-01T00:00:00+0000");
        }
        assertEquals(dobDate, response.dateOfBirth);

        assertEquals(BigDecimal.valueOf(3.5), response.weight);

        assertEquals("Test Comment", response.comments);
        assertEquals(Integer.valueOf(87435), response.householdId);
        assertEquals(Integer.valueOf(23412), response.photoId);
        assertEquals(SurePetcarePet.PetSpecies.CAT.id, response.speciesId);
        assertEquals(Integer.valueOf(382), response.breedId);

        assertEquals(Integer.valueOf(1), response.status.activity.where);
        Date sinceDate;
        synchronized (simpleDateFormat) {
            sinceDate = simpleDateFormat.parse("2019-10-03T10:23:37+0000");
        }
        assertEquals(sinceDate, response.status.activity.since);
    }

}
