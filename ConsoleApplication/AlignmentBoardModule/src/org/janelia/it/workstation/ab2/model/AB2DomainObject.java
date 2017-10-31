package org.janelia.it.workstation.ab2.model;

import java.util.Date;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;

public class AB2DomainObject implements DomainObject
{

    ////////////////////////////////////////////////////////////////////////////////////
    /// From DomainObject
    ////////////////////////////////////////////////////////////////////////////////////

    /** Returns a Globally Unique Identifier for the object */
    public Long getId() {
        return null;
    }

    public void setId(Long id) {}

    /** Returns a user-readable, non-unique label for the object instance */
    public String getName() { return AB2SkeletonDomainObject.class.getName(); }

    public void setName(String name) {}

    /** Returns the key for the subject who knows the object instance */
    public String getOwnerKey() { return null; }

    public void setOwnerKey(String ownerKey) {}

    /** Returns all the keys of subjects who have read access to the object instance */
    public Set<String> getReaders() { return null; }

    public void setReaders(Set<String> readers) {}

    /** Returns all the keys of subjects who have write access to the object instance */
    public Set<String> getWriters() { return null; }

    public void setWriters(Set<String> writers) {}

    /** Returns the date/time when the object was created */
    public Date getCreationDate() { return null; }

    public void setCreationDate(Date creationDate) {}

    /** Returns the date/time when the object was last updated */
    public Date getUpdatedDate() { return null; }

    public void setUpdatedDate(Date updatedDate) {}

    /** Returns a user-readable label for the domain object sub-type */
    public String getType() { return null; }

}