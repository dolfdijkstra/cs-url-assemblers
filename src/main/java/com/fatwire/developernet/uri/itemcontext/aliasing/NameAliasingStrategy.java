/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.itemcontext.aliasing;

import COM.FutureTense.Interfaces.*;
import COM.FutureTense.Util.IterableIListWrapper;
import COM.FutureTense.Util.ftErrors;
import com.fatwire.assetapi.common.AssetAccessException;
import com.fatwire.assetapi.data.*;
import com.fatwire.developernet.CSRuntimeException;
import static com.fatwire.developernet.IListUtils.getLongValue;
import static com.fatwire.developernet.facade.mda.DimensionUtils.getLocaleAsDimension;
import com.fatwire.developernet.facade.runtag.example.asset.AssetList;
import com.fatwire.mda.Dimension;
import com.fatwire.system.SessionFactory;
import com.openmarket.xcelerate.asset.AssetIdImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * This translator simply uses the <code>name</code> attribute of the
 * specified asset to look up the value to be used in the URL.
 * <p/>
 * This is not the prettiest assembler to use but it is guaranteed to
 * always be set so it is good for testing.  It does not require any
 * configuration
 *
 * @author Tony Field
 * @since Jun 1, 2009
 */
public class NameAliasingStrategy implements AssetAliasingStrategy
{
    private static Log LOG = LogFactory.getLog(NameAliasingStrategy.class.getName());
    private final ICS ics;

    public NameAliasingStrategy(ICS ics) { this.ics = ics; }

    public String computeAlias(AssetId id, String localeName)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("PathAliasingStrategy.computeAlias: Computing cpath for " + id + " in locale " + localeName);
        }
        AssetDataManager mgr = (AssetDataManager)SessionFactory.getSession(ics).getManager(AssetDataManager.class.getName());
        AssetData asset;
        try
        {
            asset = mgr.readAttributes(id, Arrays.asList("name"));
        }
        catch(AssetAccessException e)
        {
            throw new RuntimeException("could not read path attribute", e);
        }
        AttributeData name = asset.getAttributeData("name");
        String result = name == null ? null : (String)name.getData();
        LOG.trace("PathAliasingStrategy.comoputeCpath: found name: " + result);
        return result;
    }

    public List<CandidateInfo> findCandidatesForAlias(String c, String cpath)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("PathAliasingStrategy.findCandidatesForAlias: Attempting to find candidates for c:cpath: " + c + ":" + cpath);
        }

        if(!Utilities.goodString(c))
        {
            throw new CSRuntimeException("Invalid c specified in findCandidatesForAlias", ftErrors.badparams);
        }
        if(!Utilities.goodString(cpath))
        {
            throw new CSRuntimeException("Invalid cpath specified in findCandidatesForAlias", ftErrors.badparams);
        }

        // start with the last element in ppath.
        AssetList assetList = new AssetList();
        assetList.setType(c);
        assetList.setList("__out");
        assetList.setExcludeVoided(true);
        assetList.setField("name", cpath);
        assetList.execute(ics);

        if(ics.GetErrno() < 0 && ics.GetErrno() != ftErrors.norows)
        {
            throw new CSRuntimeException("Error looking up assets of type: " + c + " by cpath: " + cpath, ics.GetErrno());
        }

        ArrayList<CandidateInfo> result = new ArrayList<CandidateInfo>();
        IList pages = ics.GetList("__out");
        ics.RegisterList("__out", null);
        if(pages == null || !pages.hasData() || pages.numRows() == 0)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("PathAliasingStrategy.findCandidatesForAlias: Could not find any assets of type: " + c + " with cpath: " + cpath);
            }
        }
        else
        {

            for(IList row : new IterableIListWrapper(pages))
            {
                AssetId id = new AssetIdImpl(c, getLongValue(row, "id"));
                Dimension dim = getLocaleAsDimension(ics, id);
                CandidateInfo ci = new CandidateInfo(id, dim);
                result.add(ci);
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("PathAliasingStrategy.findCandidatesForAlias: Found possible match for c:" + c + ", cpath:" + cpath + ": " + ci);
                }
            }
        }
        return result;
    }
}
