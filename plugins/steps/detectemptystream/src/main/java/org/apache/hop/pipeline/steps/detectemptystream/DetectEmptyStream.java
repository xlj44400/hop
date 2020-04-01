/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.pipeline.steps.detectemptystream;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.step.BaseStep;
import org.apache.hop.pipeline.step.StepDataInterface;
import org.apache.hop.pipeline.step.StepInterface;
import org.apache.hop.pipeline.step.StepMeta;
import org.apache.hop.pipeline.step.StepMetaInterface;

/**
 * Detect empty stream. Pass one row data to the next steps.
 *
 * @author Samatar
 * @since 30-08-2008
 */
public class DetectEmptyStream extends BaseStep implements StepInterface {
  private static Class<?> PKG = DetectEmptyStreamMeta.class; // for i18n purposes, needed by Translator!!

  private DetectEmptyStreamData data;

  public DetectEmptyStream( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
                            PipelineMeta pipelineMeta, Pipeline pipeline ) {
    super( stepMeta, stepDataInterface, copyNr, pipelineMeta, pipeline );
  }

  /**
   * Build an empty row based on the meta-data.
   *
   * @return
   */
  private Object[] buildOneRow() throws HopStepException {
    // return previous fields name
    Object[] outputRowData = RowDataUtil.allocateRowData( data.outputRowMeta.size() );
    return outputRowData;
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws HopException {
    data = (DetectEmptyStreamData) sdi;

    Object[] r = getRow(); // get row, set busy!
    if ( r == null ) { // no more input to be expected...

      if ( first ) {
        // input stream is empty !
        data.outputRowMeta = getPipelineMeta().getPrevStepFields( getStepMeta() );
        putRow( data.outputRowMeta, buildOneRow() ); // copy row to possible alternate rowset(s).

        if ( checkFeedback( getLinesRead() ) ) {
          if ( log.isBasic() ) {
            logBasic( BaseMessages.getString( PKG, "DetectEmptyStream.Log.LineNumber" ) + getLinesRead() );
          }
        }
      }
      setOutputDone();
      return false;
    }

    if ( first ) {
      first = false;
    }

    return true;
  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    data = (DetectEmptyStreamData) sdi;

    if ( super.init( smi, sdi ) ) {
      // Add init code here.
      return true;
    }
    return false;
  }

}