/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.blocks;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.ktx.KtxData;
import org.janelia.horta.ktx.KtxHeader;
import org.openide.util.Exceptions;
import org.python.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
public class KtxOctreeBlockTileSource implements BlockTileSource 
{
    private final URL rootUrl;
    
    private final KtxHeader rootHeader;
    // private final File rootFolder;
    private final KtxOctreeResolution maximumResolution;
    
    private final ConstVector3 origin;
    private final Vector3 outerCorner;
    private final KtxOctreeBlockTileKey rootKey;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Like "1/2/3/3"
    private String subfolderForKey(KtxOctreeBlockTileKey key) {
        String result = key.toString();
        if (result.length() > 0)
            result = result + "/"; // Add trailing (but not initial) slash
        return result;
    }
    
    private URL folderForKey(BlockTileKey key0) 
    {
        KtxOctreeBlockTileKey key = (KtxOctreeBlockTileKey) key0;
        String subfolderStr = subfolderForKey(key);
        URL folderUrl = null;
        try {
            folderUrl = new URL(rootUrl, subfolderStr);
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return folderUrl;
    }
    
    private URL blockUrlForKey(KtxOctreeBlockTileKey key) throws IOException {
        URL folder = folderForKey(key);

        // TODO: Main defect of URL vs. File is searchability of actual file name.
        // block_2016-07-18b_8_xy_.ktx
        String[] pathParts = rootUrl.getPath().split("/");
        String specimenName = pathParts[pathParts.length - 1];
        String subfolderStr = subfolderForKey(key);
        subfolderStr = subfolderStr.replaceAll("/", "");
        String fileName = "block_" + specimenName + "_8_xy_" + subfolderStr + ".ktx";
        
        URL blockUrl = new URL(folder, fileName);
        return blockUrl;
    }
    
    public InputStream streamForKey(BlockTileKey key) throws IOException {
        URL url = blockUrlForKey((KtxOctreeBlockTileKey)key);
        return new BufferedInputStream(url.openStream());
    }
    
    public KtxOctreeBlockTileSource(URL rootUrl) throws IOException 
    {
        this.rootUrl = rootUrl;
        rootKey = new KtxOctreeBlockTileKey(new ArrayList<Integer>(), this);
        
        try (InputStream stream = streamForKey(rootKey)) {
            rootHeader = new KtxHeader();
            rootHeader.loadStream(stream);
        }
        
        // Parse maximum resolution
        int maxRes = Integer.parseInt(rootHeader.keyValueMetadata.get("multiscale_total_levels").trim()) - 1;
        maximumResolution = new KtxOctreeResolution(maxRes);
        
        // Parse outer corner locations of volume block
        // Trivia note: All this parsing would be a one-liner in PERL.
        String cornersString = rootHeader.keyValueMetadata.get("corner_xyzs").trim();
        // logger.info(cornersString);
        // [(68097.320000000007, 13754.192000000001, 27557.100000000002), (79094.79800000001, 13754.192000000001, 27557.100000000002), (68097.320000000007, 21962.162, 27557.100000000002), (79094.79800000001, 21962.162, 27557.100000000002), (68097.320000000007, 13754.192000000001, 42164.300000000003), (79094.79800000001, 13754.192000000001, 42164.300000000003), (68097.320000000007, 21962.162, 42164.300000000003), (79094.79800000001, 21962.162, 42164.300000000003)]
        String numberPattern = "[-+]?[0-9]+(?:\\.[0-9]+)?";
        String tuple3Pattern = "\\((" + numberPattern + ", " + numberPattern + ", " + numberPattern + ")\\)";
        // Extract just the first and last corner locations from the corner list
        Pattern p = Pattern.compile("^\\[" + tuple3Pattern + ".*" + tuple3Pattern + "\\]$");
        Matcher m = p.matcher(cornersString);
        if (! m.matches()) {
            throw new IOException("Error parsing Ktx block corners");
        }
        // logger.info("Matches");
        // logger.info("" + m.groupCount());
        // logger.info(m.group(1));
        // logger.info(m.group(2));
        String[] originStrings = m.group(1).split(", ");
        String[] outerCornerStrings = m.group(2).split(", ");
        origin = new Vector3(
                Float.parseFloat(originStrings[0]),
                Float.parseFloat(originStrings[1]),
                Float.parseFloat(originStrings[2]));
        outerCorner = new Vector3(
                Float.parseFloat(outerCornerStrings[0]),
                Float.parseFloat(outerCornerStrings[1]),
                Float.parseFloat(outerCornerStrings[2]));
        // logger.info("" + origin);
        // logger.info("" + outerCorner);
        assert origin.getX() < outerCorner.getX();
        assert origin.getY() < outerCorner.getY();
        assert origin.getZ() < outerCorner.getZ();
    }

    @Override
    public BlockTileResolution getMaximumResolution() {
        return maximumResolution;
    }

    @Override
    public BlockTileKey getBlockKeyAt(ConstVector3 location, BlockTileResolution resolution0) 
    {
        if (resolution0 == null)
            resolution0 = getMaximumResolution();
        KtxOctreeResolution resolution = (KtxOctreeResolution)resolution0;
        if (resolution.compareTo(getMaximumResolution()) > 0) 
            return null; // no resolution that high
        
        if (location.getX() < origin.getX()) return null;
        if (location.getY() < origin.getY()) return null;
        if (location.getZ() < origin.getZ()) return null;
        
        if (location.getX() > outerCorner.getX()) return null;
        if (location.getY() > outerCorner.getY()) return null;
        if (location.getZ() > outerCorner.getZ()) return null;
        
        List<Integer> octreePath = new ArrayList<>();
        Vector3 subBlockOrigin = new Vector3(origin);
        Vector3 subBlockExtent = outerCorner.minus(origin);
        while (octreePath.size() < resolution.octreeLevel ) 
        {
            // Reduce block size to half, per octree level
            subBlockExtent.setX(subBlockExtent.getX()/2.0f);
            subBlockExtent.setY(subBlockExtent.getY()/2.0f);
            subBlockExtent.setZ(subBlockExtent.getZ()/2.0f);

            int octreeStep = 1;
            if (location.getX() > subBlockOrigin.getX() + subBlockExtent.getX()) 
            { // larger X
                octreeStep += 1;
                subBlockOrigin.setX(subBlockOrigin.getX() + subBlockExtent.getX());
            }
            if (location.getY() > subBlockOrigin.getY() + subBlockExtent.getY()) 
            { // larger Y
                octreeStep += 2;
                subBlockOrigin.setY(subBlockOrigin.getY() + subBlockExtent.getY());
            }
            if (location.getZ() > subBlockOrigin.getZ() + subBlockExtent.getZ()) 
            { // larger Z
                octreeStep += 4;
                subBlockOrigin.setZ(subBlockOrigin.getZ() + subBlockExtent.getZ());
            }
            
            octreePath.add(octreeStep);
        }
        
        return new KtxOctreeBlockTileKey(octreePath, this);
    }

    @Override
    public BlockTileKey getClosestTileKey(ConstVector3 focus, BlockTileResolution resolution) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BlockTileKey getBlockKeyAdjacent(BlockTileKey centerBlock, int dx, int dy, int dz) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ConstVector3 getBlockCentroid(BlockTileKey centerBlock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean blockExists(BlockTileKey key) throws IOException
    {
        URL blockUrl = blockUrlForKey((KtxOctreeBlockTileKey)key);
        try (InputStream stream = blockUrl.openStream()) { // throws IOException
            return true;
        } catch (FileNotFoundException ex) {
            return false;
            // Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            return false;
            // Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public BlockTileData loadBlock(BlockTileKey key) 
            throws IOException, InterruptedException
    {
        long t0 = System.nanoTime();
        try (InputStream stream = streamForKey(key)) {
            KtxOctreeBlockTileData data = new KtxOctreeBlockTileData();
            data.loadStreamInterruptably(stream);
            long t1 = System.nanoTime();
            float elapsed = (t1 - t0) / 1e9f;
            logger.info("Ktx tile '" + key + "' stream load took " 
                    + new DecimalFormat("#0.000").format(elapsed) 
                    + "seconds");
            return data;
        }
    }

    @Override
    public URL getRootUrl() {
        return rootUrl;
    }
    
    static public class KtxOctreeBlockTileData
    extends KtxData
    implements BlockTileData
    {
        
    }
    
    
    static public class KtxOctreeBlockTileKey
    implements BlockTileKey
    {
        private final KtxOctreeBlockTileSource source;
        private final List<Integer> path;

        private KtxOctreeBlockTileKey(List<Integer> octreePath, KtxOctreeBlockTileSource source) {
            this.source = source;
            this.path = octreePath;
        }

        @Override
        public ConstVector3 getCentroid() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public BlockTileSource getSource() {
            return source;
        }
        
        @Override
        public String toString() {
            List<String> steps = new ArrayList<>();
            for (Integer s : path) {
                steps.add(s.toString());
            }
            String subfolderStr = Joiner.on("/").join(steps);
            return subfolderStr;  
        }
    }
    
    
    static public class KtxOctreeResolution 
    implements BlockTileResolution
    {
        final int octreeLevel; // zero-based level; zero means tip of pyramid

        public KtxOctreeResolution(int octreeLevel) {
            this.octreeLevel = octreeLevel;
        }

        @Override
        public int hashCode() {
            return octreeLevel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KtxOctreeResolution other = (KtxOctreeResolution) obj;
            if (this.octreeLevel != other.octreeLevel) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(BlockTileResolution o) {
            KtxOctreeResolution rhs = (KtxOctreeResolution)o;
            return octreeLevel < rhs.octreeLevel ? -1 : octreeLevel > rhs.octreeLevel ? 1 : 0;
        }
        
    }
    
}
