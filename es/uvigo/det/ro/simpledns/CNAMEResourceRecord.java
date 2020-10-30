/*
 * Copyright (C) 2016 Miguel Rodriguez Perez <miguel@det.uvigo.gal>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
 //Clase realizada a partir de la de NS
 
package es.uvigo.det.ro.simpledns;

import static es.uvigo.det.ro.simpledns.RRType.CNAME;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Miguel Rodriguez Perez
 */
public class CNAMEResourceRecord extends ResourceRecord {
    private final DomainName cname;

    public CNAMEResourceRecord(DomainName domain, int ttl, DomainName cname) {
        super(domain, CNAME, ttl, cname.toByteArray());
        
        this.cname = cname;
    }

    protected CNAMEResourceRecord(ResourceRecord decoded, final byte[] message) {
        super(decoded);

        cname = new DomainName(getRRData(), message);
    }

    public final DomainName getCNAME() {
        return cname;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        try {        
            os.write(super.toByteArray());
            os.write(cname.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(CNAMEResourceRecord.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }        
        
        return os.toByteArray();
    }
}
