/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.itemcontext.aliasing;

import COM.FutureTense.Interfaces.ICS;
import com.fatwire.assetapi.data.AssetId;
import com.fatwire.mda.Dimension;
import com.openmarket.xcelerate.asset.AssetIdImpl;

import java.util.Arrays;
import java.util.List;

/**
 * Trivial aliasing strategy that uses the asset's ID as its alias.  Normally this
 * would never be used but since it is guaranteed to always work, it exists for
 * prototyping, testing, and demonstration purposes.  It does not require any configuration.
 *
 * @author Tony Field
 * @since Jul 23, 2009
 */
public final class IdAliasingStrategy implements AssetAliasingStrategy
{
    public IdAliasingStrategy(ICS ics) { }

    /**
     * The ID of the input asset is returned as the alias
     *
     * @param id Asset ID
     * @param localeName name of locale to find the path for.  Null is allowed,
     * in which case to translation is sought.
     * @return the ID of the asset
     */
    public String computeAlias(AssetId id, String localeName)
    {
        return Long.toString(id.getId());
    }

    /**
     * Returns a list containing a single entry - the ID of the asset
     * whose alias is the ID itself.
     *
     * @param type asset type
     * @param alias alias for the asset
     * @return list of results
     */
    public List<CandidateInfo> findCandidatesForAlias(String type, String alias)
    {
        long id = Long.valueOf(alias);
        Dimension dim = null;
        return Arrays.asList(new CandidateInfo(new AssetIdImpl(type, id), dim));
    }
}
