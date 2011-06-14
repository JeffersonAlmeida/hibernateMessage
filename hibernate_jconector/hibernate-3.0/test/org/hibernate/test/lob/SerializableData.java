// $Id: SerializableData.java,v 1.1 2004/11/04 21:59:22 steveebersole Exp $
package org.hibernate.test.lob;

import java.io.Serializable;

/**
 * Implementation of SerializableData.
 *
 * @author Steve
 */
public class SerializableData implements Serializable
{
   private String payload;

   public SerializableData(String payload)
   {
      this.payload = payload;
   }

   public String getPayload()
   {
      return payload;
   }

   public void setPayload(String payload)
   {
      this.payload = payload;
   }
}
