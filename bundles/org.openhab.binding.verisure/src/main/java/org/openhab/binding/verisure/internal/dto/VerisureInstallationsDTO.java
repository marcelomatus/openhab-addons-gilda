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
package org.openhab.binding.verisure.internal.dto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.verisure.internal.dto.VerisureBaseThingDTO.Installation;

import com.google.gson.annotations.SerializedName;

/**
 * The installation(s) of the Verisure System.
 *
 * @author Jan Gustafsson - Initial contribution
 *
 */
@NonNullByDefault
public class VerisureInstallationsDTO {

    private Data data = new Data();

    public Data getData() {
        return data;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Data)) {
            return false;
        }
        VerisureInstallationsDTO rhs = ((VerisureInstallationsDTO) other);
        return new EqualsBuilder().append(data, rhs.data).isEquals();
    }

    public static class Data {
        private Installation installation = new Installation();
        private Account account = new Account();

        public Account getAccount() {
            return account;
        }

        public Installation getInstallation() {
            return installation;
        }

        public void setInstallation(Installation installation) {
            this.installation = installation;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("installation", installation).append("account", account).toString();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Data)) {
                return false;
            }
            Data rhs = ((Data) other);
            return new EqualsBuilder().append(installation, rhs.installation).isEquals();
        }
    }

    public static class Account {

        @SerializedName("__typename")
        private @Nullable String typename;
        private List<Owainstallation> owainstallations = new ArrayList<>();

        public @Nullable String getTypename() {
            return typename;
        }

        public List<Owainstallation> getOwainstallations() {
            return owainstallations;
        }

        @Override
        public String toString() {
            return owainstallations.size() > 0 ? new ToStringBuilder(this).append("owainstallations", owainstallations)
                    .append("typename", typename).toString() : "";
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Account)) {
                return false;
            }
            Account rhs = ((Account) other);
            return new EqualsBuilder().append(typename, rhs.typename).append(owainstallations, rhs.owainstallations)
                    .isEquals();
        }
    }

    public static class Owainstallation {

        @SerializedName("__typename")
        private @Nullable String typename;
        private @Nullable String alias;
        private @Nullable String dealerId;
        private @Nullable String giid;
        private @Nullable Object subsidiary;
        private @Nullable String type;

        public @Nullable String getTypename() {
            return typename;
        }

        public @Nullable String getAlias() {
            return alias;
        }

        public @Nullable String getDealerId() {
            return dealerId;
        }

        public @Nullable String getGiid() {
            return giid;
        }

        public @Nullable Object getSubsidiary() {
            return subsidiary;
        }

        public @Nullable String getType() {
            return type;
        }

        @Override
        public String toString() {
            return typename != null ? new ToStringBuilder(this).append("giid", giid).append("alias", alias)
                    .append("type", type).append("subsidiary", subsidiary).append("dealerId", dealerId)
                    .append("typename", typename).toString() : "";
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Owainstallation)) {
                return false;
            }
            Owainstallation rhs = ((Owainstallation) other);
            return new EqualsBuilder().append(dealerId, rhs.dealerId).append(alias, rhs.alias)
                    .append(typename, rhs.typename).append(giid, rhs.giid).append(subsidiary, rhs.subsidiary)
                    .append(type, rhs.type).isEquals();
        }
    }
}
