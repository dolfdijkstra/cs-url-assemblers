/*
* Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
* Title, ownership rights, and intellectual property rights in and
* to this software remain with FatWire Corporation. This  software
* is protected by international copyright laws and treaties, and
* may be protected by other law.  Violation of copyright laws may
* result in civil liability and criminal penalties.
*/
package com.fatwire.developernet.uri.itemcontext.aliasing;

import com.fatwire.assetapi.data.AssetId;
import com.fatwire.mda.Dimension;

/**
 * Container relating the asset ID that is a candidate for having created part of the path, with
 * the effective dimension used to create it.
 *
 * @author Tony Field
 * @since Jun 1, 2009
 */
public class CandidateInfo
{
    private final Dimension dim;
    private final AssetId id;

    public CandidateInfo(AssetId id, Dimension dim)
    {
        if(id == null) { throw new IllegalArgumentException("Null asset id not allowed"); }
        this.dim = dim;
        this.id = id;
    }

    public Dimension getDim()
    {
        return dim;
    }

    public AssetId getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof CandidateInfo))
        {
            return false;
        }

        CandidateInfo that = (CandidateInfo)o;

        if(dim != null ? !dim.equals(that.dim) : that.dim != null)
        {
            return false;
        }
        if(!id.equals(that.id))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = dim != null ? dim.hashCode() : 0;
        result = 31 * result + id.hashCode();
        return result;
    }

    public String toString() { return "{" + id + "-" + (dim == null ? "no_LOCALE" : dim.getName()) + "}"; }
}