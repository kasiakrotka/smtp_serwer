package com.company.smtp.client;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;

public class MXLookup {

    public ArrayList doLookup( String hostName ) throws NamingException {
            // Perform a DNS lookup for MX records in the domain
            Hashtable env = new Hashtable();
            env.put("java.naming.factory.initial",
                    "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext( env );
            Attributes attrs = ictx.getAttributes
                    ( hostName, new String[] { "MX" });
            Attribute attr = attrs.get( "MX" );

            // if we don't have an MX record, try the machine itself
            if (( attr == null ) || ( attr.size() == 0 )) {
                attrs = ictx.getAttributes( hostName, new String[] { "A" });
                attr = attrs.get( "A" );
                if( attr == null )
                    throw new NamingException
                            ( "No match for name '" + hostName + "'" );
            }

            // Huzzah! we have machines to try. Return them as an array list
            // NOTE: We SHOULD take the preference into account to be absolutely
            //   correct. This is left as an exercise for anyone who cares.
            ArrayList res = new ArrayList();
            NamingEnumeration en = attr.getAll();

            while ( en.hasMore() ) {
                String x = (String) en.next();
                String f[] = x.split( " " );
                if ( f[1].endsWith( "." ) )
                    f[1] = f[1].substring( 0, (f[1].length() - 1));
                res.add( f[1] );
            }
            return res;
    }
}