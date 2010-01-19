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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface encapsulating the raw capability to translate an asset ID
 * into a string to be used as part of a URL, and back.
 * <p/>
 * Constructors must take a single argument, ICS.
 *
 * @author Tony Field
 * @author Matthew Soh
 * @since Jan 19, 2010
 */
public interface AssetAliasingStrategy
{
    /**
     * Given an asset ID, compute the alias for that asset.  It is not an
     * error if it is not possible to return an alias.  In that case,
     * null is returned.
     *
     * @param id Asset ID
     * @param localeName name of locale to find the path for.  Null is allowed,
     * in which case to translation is sought.
     * @return alias string fragment.  If null is returned, it means that
     * no alias could be computed.
     */
    String computeAlias(AssetId id, String localeName);

    /**
     * This method attempts to resolve an asset ID from
     * an input item type name and alias.
     *
     * @param type asset type
     * @param alias alias for the asset
     * @return list of matching asset IDs, never null.  List may return now rows.
     */
    List<CandidateInfo> findCandidatesForAlias(String type, String alias);
    
    
    /**
     * Characters that are not allowed in an alias. Aliasing strategies may ignore this if it does not apply.
     */
    public static final Pattern ILLEGAL_CHARACTER_PATTERN = Pattern.compile("[~!@#\\$%\\^&*\\(\\)+=\\{\\}\\|\\[\\]\\\\:\";'<>?,./ ]");
}


