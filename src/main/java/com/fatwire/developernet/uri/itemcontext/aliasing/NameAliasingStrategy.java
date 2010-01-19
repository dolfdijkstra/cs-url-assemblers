/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.itemcontext.aliasing;

import static com.fatwire.developernet.IListUtils.getLongValue;
import static com.fatwire.developernet.facade.mda.DimensionUtils.getLocaleAsDimension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import COM.FutureTense.Interfaces.ICS;
import COM.FutureTense.Interfaces.IList;
import COM.FutureTense.Interfaces.Utilities;
import COM.FutureTense.Util.IterableIListWrapper;
import COM.FutureTense.Util.ftErrors;

import com.fatwire.assetapi.common.AssetAccessException;
import com.fatwire.assetapi.data.AssetData;
import com.fatwire.assetapi.data.AssetDataManager;
import com.fatwire.assetapi.data.AssetId;
import com.fatwire.assetapi.data.AttributeData;
import com.fatwire.cs.core.db.PreparedStmt;
import com.fatwire.cs.core.db.StatementParam;
import com.fatwire.developernet.CSRuntimeException;
import com.fatwire.mda.Dimension;
import com.fatwire.system.SessionFactory;
import com.openmarket.xcelerate.asset.AssetIdImpl;

/**
 * This translator simply uses the <code>name</code> attribute of the
 * specified asset to look up the value to be used in the URL.
 * <p/>
 * This is not the prettiest assembler to use but it is guaranteed to
 * always be set so it is good for testing.  It does not require any
 * configuration
 *
 * @author Tony Field
 * @author Matthew Soh
 * @since Jan 19, 2010
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
            LOG.trace("NameAliasingStrategy.computeAlias: Computing cpath for " + id + " in locale " + localeName);
        }
        AssetDataManager mgr = (AssetDataManager)SessionFactory.getSession(ics).getManager(AssetDataManager.class.getName());
        AssetData asset;
        try
        {
            asset = mgr.readAttributes(id, Arrays.asList("name"));
        }
        catch(AssetAccessException e)
        {
            throw new RuntimeException("could not read name attribute", e);
        }
        AttributeData name = asset.getAttributeData("name");
        String result = name == null ? null : (String)name.getData();
        LOG.trace("NameAliasingStrategy.computeAlias: found name: " + result);
        return result;
    }

    public List<CandidateInfo> findCandidatesForAlias(String c, String cpath)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("NameAliasingStrategy.findCandidatesForAlias: Attempting to find candidates for c:cpath: " + c + ":" + cpath);
        }

        if(!Utilities.goodString(c))
        {
            throw new CSRuntimeException("Invalid c specified in findCandidatesForAlias", ftErrors.badparams);
        }
        if(!Utilities.goodString(cpath))
        {
            throw new CSRuntimeException("Invalid cpath specified in findCandidatesForAlias", ftErrors.badparams);
        }

        /*
        // start with the last element in ppath.
        AssetList assetList = new AssetList();
        assetList.setType(c);
        assetList.setList("__out");
        assetList.setExcludeVoided(true);
        assetList.setField("name", cpath);
        assetList.execute(ics);
		*/
        
        ArrayList<String> tables = new ArrayList<String>(1);
        tables.add(c);
        PreparedStmt ps = new PreparedStmt( "select id from " + c + "  where lower(name) = ? and status != 'VO'", tables);
        ps.setElement(0, c, "name");
        
        StatementParam sp = ps.newParam();
        sp.setString(0, cpath);
        
        IList lstResult =  ics.SQL(ps, sp, true); 
        		
        if(ics.GetErrno() < 0 && ics.GetErrno() != ftErrors.norows)
        {
            throw new CSRuntimeException("Error looking up assets of type: " + c + " by cpath: " + cpath, ics.GetErrno());
        }

        ArrayList<CandidateInfo> result = new ArrayList<CandidateInfo>();
        if(lstResult == null || !lstResult.hasData() || lstResult.numRows() == 0)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("NameAliasingStrategy.findCandidatesForAlias: Could not find any assets of type: " + c + " with cpath: " + cpath);
            }
        }
        else
        {
            for(IList row : new IterableIListWrapper(lstResult))
            {
                AssetId id = new AssetIdImpl(c, getLongValue(row, "id"));
                Dimension dim = getLocaleAsDimension(ics, id);
                CandidateInfo ci = new CandidateInfo(id, dim);
                result.add(ci);
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("NameAliasingStrategy.findCandidatesForAlias: Found possible match for c:" + c + ", cpath:" + cpath + ": " + ci);
                }
            }
        }
        return result;
    }
}
