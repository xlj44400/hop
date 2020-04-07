/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * http://www.project-hop.org
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

package org.apache.hop.pipeline.transforms.synchronizeaftermerge;

import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.Const;
import org.apache.hop.core.SQLStatement;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.injection.AfterInjection;
import org.apache.hop.core.injection.Injection;
import org.apache.hop.core.injection.InjectionSupported;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.iVariables;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.DatabaseImpact;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.ITransform;
import org.w3c.dom.Node;

import java.util.List;

/*
 * Created on 13-10-2008
 *
 */
@InjectionSupported( localizationPrefix = "SynchronizeAfterMerge.Injection.", groups = { "KEYS_TO_LOOKUP",
  "UPDATE_FIELDS" } )
public class SynchronizeAfterMergeMeta extends BaseTransformMeta implements ITransform {
  private static Class<?> PKG = SynchronizeAfterMergeMeta.class; // for i18n purposes, needed by Translator!!

  /**
   * what's the lookup schema?
   */
  @Injection( name = "SHEMA_NAME" )
  private String schemaName;

  /**
   * what's the lookup table?
   */
  @Injection( name = "TABLE_NAME" )
  private String tableName;

  private IMetaStore metaStore;

  /**
   * database connection
   */
  private DatabaseMeta databaseMeta;

  /**
   * which field in input stream to compare with?
   */
  @Injection( name = "STREAM_FIELD1", group = "KEYS_TO_LOOKUP" )
  private String[] keyStream;

  /**
   * field in table
   */
  @Injection( name = "TABLE_FIELD", group = "KEYS_TO_LOOKUP" )
  private String[] keyLookup;

  /**
   * Comparator: =, <>, BETWEEN, ...
   */
  @Injection( name = "COMPARATOR", group = "KEYS_TO_LOOKUP" )
  private String[] keyCondition;

  /**
   * Extra field for between...
   */
  @Injection( name = "STREAM_FIELD2", group = "KEYS_TO_LOOKUP" )
  private String[] keyStream2;

  /**
   * Field value to update after lookup
   */
  @Injection( name = "UPDATE_TABLE_FIELD", group = "UPDATE_FIELDS" )
  private String[] updateLookup;

  /**
   * Stream name to update value with
   */
  @Injection( name = "STREAM_FIELD", group = "UPDATE_FIELDS" )
  private String[] updateStream;

  /**
   * boolean indicating if field needs to be updated
   */
  @Injection( name = "UPDATE", group = "UPDATE_FIELDS" )
  private Boolean[] update;

  /**
   * Commit size for inserts/updates
   */
  @Injection( name = "COMMIT_SIZE" )
  private String commitSize;

  @Injection( name = "TABLE_NAME_IN_FIELD" )
  private boolean tablenameInField;

  @Injection( name = "TABLE_NAME_FIELD" )
  private String tablenameField;

  @Injection( name = "OPERATION_ORDER_FIELD" )
  private String operationOrderField;

  @Injection( name = "USE_BATCH_UPDATE" )
  private boolean useBatchUpdate;

  @Injection( name = "PERFORM_LOOKUP" )
  private boolean performLookup;

  @Injection( name = "ORDER_INSERT" )
  private String OrderInsert;

  @Injection( name = "ORDER_UPDATE" )
  private String OrderUpdate;

  @Injection( name = "ORDER_DELETE" )
  private String OrderDelete;

  public SynchronizeAfterMergeMeta() {
    super(); // allocate BaseTransformMeta
  }

  @Injection( name = "CONNECTION_NAME" )
  public void setConnection( String connectionName ) {
    try {
      databaseMeta = DatabaseMeta.loadDatabase( metaStore, connectionName );
    } catch ( HopXMLException e ) {
      throw new RuntimeException( "Error load connection '" + connectionName + "'", e );
    }
  }

  /**
   * @param useBatchUpdate The useBatchUpdate flag to set.
   */
  public void setUseBatchUpdate( boolean useBatchUpdate ) {
    this.useBatchUpdate = useBatchUpdate;
  }

  /**
   * @return Returns the useBatchUpdate flag.
   */
  public boolean useBatchUpdate() {
    return useBatchUpdate;
  }

  /**
   * @param performLookup The performLookup flag to set.
   */
  public void setPerformLookup( boolean performLookup ) {
    this.performLookup = performLookup;
  }

  /**
   * @return Returns the performLookup flag.
   */
  public boolean isPerformLookup() {
    return performLookup;
  }

  public boolean istablenameInField() {
    return tablenameInField;
  }

  public void settablenameInField( boolean tablenamefield ) {
    this.tablenameInField = tablenamefield;
  }

  public String gettablenameField() {
    return tablenameField;
  }

  public String getOperationOrderField() {
    return operationOrderField;
  }

  public String getOrderInsert() {
    return OrderInsert;
  }

  public String getOrderUpdate() {
    return OrderUpdate;
  }

  public String getOrderDelete() {
    return OrderDelete;
  }

  public void setOrderInsert( String insert ) {
    this.OrderInsert = insert;
  }

  public void setOrderUpdate( String update ) {
    this.OrderUpdate = update;
  }

  public void setOrderDelete( String delete ) {
    this.OrderDelete = delete;
  }

  public void setOperationOrderField( String operationOrderField ) {
    this.operationOrderField = operationOrderField;
  }

  public void settablenameField( String tablenamefield ) {
    this.tablenameField = tablenamefield;
  }

  /**
   * @return Returns the commitSize.
   */
  public String getCommitSize() {
    return commitSize;
  }

  /**
   * @param commitSize The commitSize to set.
   */
  public void setCommitSize( int commitSize ) {
    this.commitSize = Integer.toString( commitSize );
  }

  /**
   * @param commitSize The commitSize to set.
   */
  public void setCommitSize( String commitSize ) {
    this.commitSize = commitSize;
  }

  /**
   * @return Returns the database.
   */
  public DatabaseMeta getDatabaseMeta() {
    return databaseMeta;
  }

  /**
   * @param database The database to set.
   */
  public void setDatabaseMeta( DatabaseMeta database ) {
    this.databaseMeta = database;
  }

  /**
   * @return Returns the keyCondition.
   */
  public String[] getKeyCondition() {
    return keyCondition;
  }

  /**
   * @param keyCondition The keyCondition to set.
   */
  public void setKeyCondition( String[] keyCondition ) {
    this.keyCondition = keyCondition;
  }

  /**
   * @return Returns the keyLookup.
   */
  public String[] getKeyLookup() {
    return keyLookup;
  }

  /**
   * @param keyLookup The keyLookup to set.
   */
  public void setKeyLookup( String[] keyLookup ) {
    this.keyLookup = keyLookup;
  }

  /**
   * @return Returns the keyStream.
   */
  public String[] getKeyStream() {
    return keyStream;
  }

  /**
   * @param keyStream The keyStream to set.
   */
  public void setKeyStream( String[] keyStream ) {
    this.keyStream = keyStream;
  }

  /**
   * @return Returns the keyStream2.
   */
  public String[] getKeyStream2() {
    return keyStream2;
  }

  /**
   * @param keyStream2 The keyStream2 to set.
   */
  public void setKeyStream2( String[] keyStream2 ) {
    this.keyStream2 = keyStream2;
  }

  /**
   * @return Returns the tableName.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * @param tableName The tableName to set.
   */
  public void setTableName( String tableName ) {
    this.tableName = tableName;
  }

  /**
   * @return Returns the updateLookup.
   */
  public String[] getUpdateLookup() {
    return updateLookup;
  }

  /**
   * @param updateLookup The updateLookup to set.
   */
  public void setUpdateLookup( String[] updateLookup ) {
    this.updateLookup = updateLookup;
  }

  /**
   * @return Returns the updateStream.
   */
  public String[] getUpdateStream() {
    return updateStream;
  }

  /**
   * @param updateStream The updateStream to set.
   */
  public void setUpdateStream( String[] updateStream ) {
    this.updateStream = updateStream;
  }

  public Boolean[] getUpdate() {
    return update;
  }

  public void setUpdate( Boolean[] update ) {
    this.update = update;
  }

  public void normalizeAllocationFields() {
    if ( keyStream != null ) {
      int keyGroupSize = keyStream.length;
      keyLookup = normalizeAllocation( keyLookup, keyGroupSize );
      keyCondition = normalizeAllocation( keyCondition, keyGroupSize );
      keyStream2 = normalizeAllocation( keyStream2, keyGroupSize );
    }
    if ( updateLookup != null ) {
      int updateGroupSize = updateLookup.length;
      updateStream = normalizeAllocation( updateStream, updateGroupSize );
      update = normalizeAllocation( update, updateGroupSize );
    }
  }

  public void loadXML( Node transformNode, IMetaStore metaStore ) throws HopXMLException {
    readData( transformNode, metaStore );
  }

  public void allocate( int nrkeys, int nrvalues ) {
    keyStream = new String[ nrkeys ];
    keyLookup = new String[ nrkeys ];
    keyCondition = new String[ nrkeys ];
    keyStream2 = new String[ nrkeys ];
    updateLookup = new String[ nrvalues ];
    updateStream = new String[ nrvalues ];
    update = new Boolean[ nrvalues ];
  }

  public Object clone() {
    SynchronizeAfterMergeMeta retval = (SynchronizeAfterMergeMeta) super.clone();
    int nrkeys = keyStream.length;
    int nrvalues = updateLookup.length;

    retval.allocate( nrkeys, nrvalues );
    System.arraycopy( keyStream, 0, retval.keyStream, 0, nrkeys );
    System.arraycopy( keyLookup, 0, retval.keyLookup, 0, nrkeys );
    System.arraycopy( keyCondition, 0, retval.keyCondition, 0, nrkeys );
    System.arraycopy( keyStream2, 0, retval.keyStream2, 0, nrkeys );

    System.arraycopy( updateLookup, 0, retval.updateLookup, 0, nrvalues );
    System.arraycopy( updateStream, 0, retval.updateStream, 0, nrvalues );
    System.arraycopy( update, 0, retval.update, 0, nrvalues );
    return retval;
  }

  private void readData( Node transformNode, IMetaStore metaStore ) throws HopXMLException {
    this.metaStore = metaStore;
    try {
      int nrkeys, nrvalues;
      this.databases = databases;
      String con = XMLHandler.getTagValue( transformNode, "connection" );
      databaseMeta = DatabaseMeta.loadDatabase( metaStore, con );
      commitSize = XMLHandler.getTagValue( transformNode, "commit" );
      schemaName = XMLHandler.getTagValue( transformNode, "lookup", "schema" );
      tableName = XMLHandler.getTagValue( transformNode, "lookup", "table" );

      useBatchUpdate = "Y".equalsIgnoreCase( XMLHandler.getTagValue( transformNode, "use_batch" ) );
      performLookup = "Y".equalsIgnoreCase( XMLHandler.getTagValue( transformNode, "perform_lookup" ) );

      tablenameInField = "Y".equalsIgnoreCase( XMLHandler.getTagValue( transformNode, "tablename_in_field" ) );
      tablenameField = XMLHandler.getTagValue( transformNode, "tablename_field" );
      operationOrderField = XMLHandler.getTagValue( transformNode, "operation_order_field" );
      OrderInsert = XMLHandler.getTagValue( transformNode, "order_insert" );
      OrderUpdate = XMLHandler.getTagValue( transformNode, "order_update" );
      OrderDelete = XMLHandler.getTagValue( transformNode, "order_delete" );

      Node lookup = XMLHandler.getSubNode( transformNode, "lookup" );
      nrkeys = XMLHandler.countNodes( lookup, "key" );
      nrvalues = XMLHandler.countNodes( lookup, "value" );

      allocate( nrkeys, nrvalues );

      for ( int i = 0; i < nrkeys; i++ ) {
        Node knode = XMLHandler.getSubNodeByNr( lookup, "key", i );

        keyStream[ i ] = XMLHandler.getTagValue( knode, "name" );
        keyLookup[ i ] = XMLHandler.getTagValue( knode, "field" );
        keyCondition[ i ] = XMLHandler.getTagValue( knode, "condition" );
        if ( keyCondition[ i ] == null ) {
          keyCondition[ i ] = "=";
        }
        keyStream2[ i ] = XMLHandler.getTagValue( knode, "name2" );
      }

      for ( int i = 0; i < nrvalues; i++ ) {
        Node vnode = XMLHandler.getSubNodeByNr( lookup, "value", i );

        updateLookup[ i ] = XMLHandler.getTagValue( vnode, "name" );
        updateStream[ i ] = XMLHandler.getTagValue( vnode, "rename" );
        if ( updateStream[ i ] == null ) {
          updateStream[ i ] = updateLookup[ i ]; // default: the same name!
        }
        String updateValue = XMLHandler.getTagValue( vnode, "update" );
        if ( updateValue == null ) {
          // default TRUE
          update[ i ] = Boolean.TRUE;
        } else {
          if ( updateValue.equalsIgnoreCase( "Y" ) ) {
            update[ i ] = Boolean.TRUE;
          } else {
            update[ i ] = Boolean.FALSE;
          }
        }
      }
    } catch ( Exception e ) {
      throw new HopXMLException( BaseMessages.getString(
        PKG, "SynchronizeAfterMergeMeta.Exception.UnableToReadTransformMetaFromXML" ), e );
    }
  }

  public void setDefault() {
    tablenameInField = false;
    tablenameField = null;
    keyStream = null;
    updateLookup = null;
    databaseMeta = null;
    commitSize = "100";
    schemaName = "";
    tableName = BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.DefaultTableName" );
    operationOrderField = null;
    OrderInsert = null;
    OrderUpdate = null;
    OrderDelete = null;
    performLookup = false;

    int nrkeys = 0;
    int nrvalues = 0;

    allocate( nrkeys, nrvalues );

    for ( int i = 0; i < nrkeys; i++ ) {
      keyLookup[ i ] = "age";
      keyCondition[ i ] = "BETWEEN";
      keyStream[ i ] = "age_from";
      keyStream2[ i ] = "age_to";
    }

    for ( int i = 0; i < nrvalues; i++ ) {
      updateLookup[ i ] = BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.ColumnName.ReturnField" ) + i;
      updateStream[ i ] = BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.ColumnName.NewName" ) + i;
      update[ i ] = Boolean.TRUE;
    }
  }

  public String getXML() {
    normalizeAllocationFields();
    StringBuilder retval = new StringBuilder( 200 );

    retval
      .append( "    " ).append(
      XMLHandler.addTagValue( "connection", databaseMeta == null ? "" : databaseMeta.getName() ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "commit", commitSize ) );

    retval.append( "    " ).append( XMLHandler.addTagValue( "tablename_in_field", tablenameInField ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "tablename_field", tablenameField ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "use_batch", useBatchUpdate ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "perform_lookup", performLookup ) );

    retval.append( "    " ).append( XMLHandler.addTagValue( "operation_order_field", operationOrderField ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "order_insert", OrderInsert ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "order_update", OrderUpdate ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "order_delete", OrderDelete ) );

    retval.append( "    <lookup>" ).append( Const.CR );
    retval.append( "      " ).append( XMLHandler.addTagValue( "schema", schemaName ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "table", tableName ) );

    for ( int i = 0; i < keyStream.length; i++ ) {
      retval.append( "      <key>" ).append( Const.CR );
      retval.append( "        " ).append( XMLHandler.addTagValue( "name", keyStream[ i ] ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "field", keyLookup[ i ] ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "condition", keyCondition[ i ] ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "name2", keyStream2[ i ] ) );
      retval.append( "      </key>" ).append( Const.CR );
    }

    for ( int i = 0; i < updateLookup.length; i++ ) {
      retval.append( "      <value>" ).append( Const.CR );
      retval.append( "        " ).append( XMLHandler.addTagValue( "name", updateLookup[ i ] ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "rename", updateStream[ i ] ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "update", update[ i ].booleanValue() ) );
      retval.append( "      </value>" ).append( Const.CR );
    }

    retval.append( "    </lookup>" ).append( Const.CR );

    return retval.toString();
  }

  public void check( List<CheckResultInterface> remarks, PipelineMeta pipelineMeta, TransformMeta transformMeta,
                     IRowMeta prev, String[] input, String[] output, IRowMeta info, iVariables variables,
                     IMetaStore metaStore ) {
    CheckResult cr;
    String error_message = "";

    if ( databaseMeta != null ) {
      Database db = new Database( loggingObject, databaseMeta );
      db.shareVariablesWith( pipelineMeta );
      try {
        db.connect();

        if ( !Utils.isEmpty( tableName ) ) {
          cr =
            new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
              PKG, "SynchronizeAfterMergeMeta.CheckResult.TableNameOK" ), transformMeta );
          remarks.add( cr );

          boolean first = true;
          boolean error_found = false;
          error_message = "";

          // Check fields in table
          IRowMeta r = db.getTableFieldsMeta( schemaName, tableName );
          if ( r != null ) {
            cr =
              new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
                PKG, "SynchronizeAfterMergeMeta.CheckResult.TableExists" ), transformMeta );
            remarks.add( cr );

            for ( int i = 0; i < keyLookup.length; i++ ) {
              String lufield = keyLookup[ i ];
              IValueMeta v = r.searchValueMeta( lufield );
              if ( v == null ) {
                if ( first ) {
                  first = false;
                  error_message +=
                    BaseMessages.getString(
                      PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingCompareFieldsInTargetTable" )
                      + Const.CR;
                }
                error_found = true;
                error_message += "\t\t" + lufield + Const.CR;
              }
            }
            if ( error_found ) {
              cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
            } else {
              cr =
                new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
                  PKG, "SynchronizeAfterMergeMeta.CheckResult.AllLookupFieldsFound" ), transformMeta );
            }
            remarks.add( cr );

            // How about the fields to insert/update in the table?
            first = true;
            error_found = false;
            error_message = "";

            for ( int i = 0; i < updateLookup.length; i++ ) {
              String lufield = updateLookup[ i ];
              IValueMeta v = r.searchValueMeta( lufield );
              if ( v == null ) {
                if ( first ) {
                  first = false;
                  error_message +=
                    BaseMessages.getString(
                      PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingFieldsToUpdateInTargetTable" )
                      + Const.CR;
                }
                error_found = true;
                error_message += "\t\t" + lufield + Const.CR;
              }
            }
            if ( error_found ) {
              cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
            } else {
              cr =
                new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
                  PKG, "SynchronizeAfterMergeMeta.CheckResult.AllFieldsToUpdateFoundInTargetTable" ), transformMeta );
            }
            remarks.add( cr );
          } else {
            error_message =
              BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.CouldNotReadTableInfo" );
            cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
            remarks.add( cr );
          }
        }

        // Look up fields in the input stream <prev>
        if ( prev != null && prev.size() > 0 ) {
          cr =
            new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
              PKG, "SynchronizeAfterMergeMeta.CheckResult.TransformReceivingDatas", prev.size() + "" ), transformMeta );
          remarks.add( cr );

          boolean first = true;
          error_message = "";
          boolean error_found = false;

          for ( int i = 0; i < keyStream.length; i++ ) {
            IValueMeta v = prev.searchValueMeta( keyStream[ i ] );
            if ( v == null ) {
              if ( first ) {
                first = false;
                error_message +=
                  BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingFieldsInInput" )
                    + Const.CR;
              }
              error_found = true;
              error_message += "\t\t" + keyStream[ i ] + Const.CR;
            }
          }
          for ( int i = 0; i < keyStream2.length; i++ ) {
            if ( keyStream2[ i ] != null && keyStream2[ i ].length() > 0 ) {
              IValueMeta v = prev.searchValueMeta( keyStream2[ i ] );
              if ( v == null ) {
                if ( first ) {
                  first = false;
                  error_message +=
                    BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingFieldsInInput" )
                      + Const.CR;
                }
                error_found = true;
                error_message += "\t\t" + keyStream[ i ] + Const.CR;
              }
            }
          }
          if ( error_found ) {
            cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
          } else {
            cr =
              new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
                PKG, "SynchronizeAfterMergeMeta.CheckResult.AllFieldsFoundInInput" ), transformMeta );
          }
          remarks.add( cr );

          // How about the fields to insert/update the table with?
          first = true;
          error_found = false;
          error_message = "";

          for ( int i = 0; i < updateStream.length; i++ ) {
            String lufield = updateStream[ i ];
            IValueMeta v = prev.searchValueMeta( lufield );

            if ( v == null ) {
              if ( first ) {
                first = false;
                error_message +=
                  BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingInputStreamFields" )
                    + Const.CR;
              }
              error_found = true;
              error_message += "\t\t" + lufield + Const.CR;
            }
          }
          if ( error_found ) {
            cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
          } else {
            cr =
              new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
                PKG, "SynchronizeAfterMergeMeta.CheckResult.AllFieldsFoundInInput2" ), transformMeta );
            remarks.add( cr );
          }

          // --------------------------> check fields in stream and tables (type)
          // Check fields in table

          String errorMsgDiffField = "";
          boolean errorDiffLenField = false;
          String errorMsgDiffLenField = "";
          boolean errorDiffField = false;

          IRowMeta r =
            db.getTableFieldsMeta( schemaName, tableName );
          if ( r != null ) {
            for ( int i = 0; i < updateStream.length; i++ ) {
              String lufieldstream = updateStream[ i ];
              String lufieldtable = updateLookup[ i ];
              // get value from previous
              IValueMeta vs = prev.searchValueMeta( lufieldstream );
              // get value from table fields
              IValueMeta vt = r.searchValueMeta( lufieldtable );
              if ( vs != null && vt != null ) {
                if ( !vs.getTypeDesc().equalsIgnoreCase( vt.getTypeDesc() ) ) {
                  errorMsgDiffField +=
                    Const.CR
                      + "The input field [" + vs.getName() + "] ( Type=" + vs.getTypeDesc()
                      + ") is not the same as the type in the target table (Type=" + vt.getTypeDesc() + ")"
                      + Const.CR;
                  errorDiffField = true;
                } else {
                  // check Length
                  if ( ( vt.getLength() < vs.getLength() || vs.getLength() == -1 ) && vt.getLength() != -1 ) {
                    errorMsgDiffLenField +=
                      Const.CR
                        + "The input field [" + vs.getName() + "] " + "(" + vs.getTypeDesc() + ")"
                        + " has a length (" + vs.getLength() + ")"
                        + " that is higher than that in the target table (" + vt.getLength() + ")." + Const.CR;
                    errorDiffLenField = true;
                  }
                }
              }
            }
            // add error/Warning
            if ( errorDiffField ) {
              errorMsgDiffField =
                BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.FieldsTypeDifferent" )
                  + Const.CR + errorMsgDiffField;
              cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, errorMsgDiffField, transformMeta );
            }
            if ( errorDiffLenField ) {
              errorMsgDiffLenField =
                BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.FieldsLenDifferent" )
                  + Const.CR + errorMsgDiffLenField;
              cr = new CheckResult( CheckResult.TYPE_RESULT_WARNING, errorMsgDiffLenField, transformMeta );
            }
            remarks.add( cr );
          }
          // --------------------------> check fields in stream and tables (type)
        } else {
          error_message =
            BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingFieldsInInput3" )
              + Const.CR;
          cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
          remarks.add( cr );
        }
      } catch ( HopException e ) {
        error_message =
          BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.DatabaseErrorOccurred" )
            + e.getMessage();
        cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
        remarks.add( cr );
      } finally {
        db.disconnect();
      }
    } else {
      error_message = BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.CheckResult.InvalidConnection" );
      cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
      remarks.add( cr );
    }

    // See if we have input streams leading to this transform!
    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SynchronizeAfterMergeMeta.CheckResult.TransformReceivingInfoFromOtherTransforms" ), transformMeta );
      remarks.add( cr );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SynchronizeAfterMergeMeta.CheckResult.NoInputError" ), transformMeta );
      remarks.add( cr );
    }
  }

  public SQLStatement getSQLStatements( PipelineMeta pipelineMeta, TransformMeta transformMeta, IRowMeta prev,
                                        IMetaStore metaStore ) throws HopTransformException {
    SQLStatement retval = new SQLStatement( transformMeta.getName(), databaseMeta, null ); // default: nothing to do!

    if ( databaseMeta != null ) {
      if ( prev != null && prev.size() > 0 ) {
        // Copy the row
        IRowMeta tableFields = new RowMeta();

        // Now change the field names
        // the key fields
        if ( keyLookup != null ) {
          for ( int i = 0; i < keyLookup.length; i++ ) {
            IValueMeta v = prev.searchValueMeta( keyStream[ i ] );
            if ( v != null ) {
              IValueMeta tableField = v.clone();
              tableField.setName( keyLookup[ i ] );
              tableFields.addValueMeta( tableField );
            } else {
              throw new HopTransformException( "Unable to find field [" + keyStream[ i ] + "] in the input rows" );
            }
          }
        }
        // the lookup fields
        for ( int i = 0; i < updateLookup.length; i++ ) {
          IValueMeta v = prev.searchValueMeta( updateStream[ i ] );
          if ( v != null ) {
            IValueMeta vk = tableFields.searchValueMeta( updateStream[ i ] );
            if ( vk == null ) { // do not add again when already added as key fields
              IValueMeta tableField = v.clone();
              tableField.setName( updateLookup[ i ] );
              tableFields.addValueMeta( tableField );
            }
          } else {
            throw new HopTransformException( "Unable to find field [" + updateStream[ i ] + "] in the input rows" );
          }
        }

        if ( !Utils.isEmpty( tableName ) ) {
          Database db = new Database( loggingObject, databaseMeta );
          try {
            db.connect();

            String schemaTable = databaseMeta.getQuotedSchemaTableCombination( schemaName, tableName );
            String cr_table = db.getDDL( schemaTable, tableFields, null, false, null, true );

            String cr_index = "";
            String[] idx_fields = null;

            if ( keyLookup != null && keyLookup.length > 0 ) {
              idx_fields = new String[ keyLookup.length ];
              for ( int i = 0; i < keyLookup.length; i++ ) {
                idx_fields[ i ] = keyLookup[ i ];
              }
            } else {
              retval.setError( BaseMessages.getString(
                PKG, "SynchronizeAfterMergeMeta.CheckResult.MissingKeyFields" ) );
            }

            // Key lookup dimensions...
            if ( idx_fields != null
              && idx_fields.length > 0 && !db.checkIndexExists( schemaName, tableName, idx_fields ) ) {
              String indexname = "idx_" + tableName + "_lookup";
              cr_index =
                db.getCreateIndexStatement( schemaTable, indexname, idx_fields, false, false, false, true );
            }

            String sql = cr_table + cr_index;
            if ( sql.length() == 0 ) {
              retval.setSQL( null );
            } else {
              retval.setSQL( sql );
            }
          } catch ( HopException e ) {
            retval.setError( BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.ReturnValue.ErrorOccurred" )
              + e.getMessage() );
          }
        } else {
          retval.setError( BaseMessages.getString(
            PKG, "SynchronizeAfterMergeMeta.ReturnValue.NoTableDefinedOnConnection" ) );
        }
      } else {
        retval.setError( BaseMessages.getString(
          PKG, "SynchronizeAfterMergeMeta.ReturnValue.NotReceivingAnyFields" ) );
      }
    } else {
      retval.setError( BaseMessages.getString( PKG, "SynchronizeAfterMergeMeta.ReturnValue.NoConnectionDefined" ) );
    }

    return retval;
  }

  public void analyseImpact( List<DatabaseImpact> impact, PipelineMeta pipelineMeta, TransformMeta transformMeta,
                             IRowMeta prev, String[] input, String[] output, IRowMeta info,
                             IMetaStore metaStore ) throws HopTransformException {
    if ( prev != null ) {
      // Lookup: we do a lookup on the natural keys
      for ( int i = 0; i < keyLookup.length; i++ ) {
        IValueMeta v = prev.searchValueMeta( keyStream[ i ] );

        DatabaseImpact ii =
          new DatabaseImpact(
            DatabaseImpact.TYPE_IMPACT_READ, pipelineMeta.getName(), transformMeta.getName(), databaseMeta
            .getDatabaseName(), tableName, keyLookup[ i ], keyStream[ i ],
            v != null ? v.getOrigin() : "?", "", "Type = " + v.toStringMeta() );
        impact.add( ii );
      }

      // Insert update fields : read/write
      for ( int i = 0; i < updateLookup.length; i++ ) {
        IValueMeta v = prev.searchValueMeta( updateStream[ i ] );

        DatabaseImpact ii =
          new DatabaseImpact(
            DatabaseImpact.TYPE_IMPACT_READ_WRITE, pipelineMeta.getName(), transformMeta.getName(), databaseMeta
            .getDatabaseName(), tableName, updateLookup[ i ], updateStream[ i ], v != null
            ? v.getOrigin() : "?", "", "Type = " + v.toStringMeta() );
        impact.add( ii );
      }
    }
  }

  public ITransform getTransform( TransformMeta transformMeta, ITransformData data, int cnr,
                                PipelineMeta pipelineMeta, Pipeline pipeline ) {
    return new SynchronizeAfterMerge( transformMeta, this, data, cnr, pipelineMeta, pipeline );
  }

  public ITransformData getTransformData() {
    return new SynchronizeAfterMergeData();
  }

  public DatabaseMeta[] getUsedDatabaseConnections() {
    if ( databaseMeta != null ) {
      return new DatabaseMeta[] { databaseMeta };
    } else {
      return super.getUsedDatabaseConnections();
    }
  }

  public IRowMeta getRequiredFields( iVariables variables ) throws HopException {
    String realTableName = variables.environmentSubstitute( tableName );
    String realSchemaName = variables.environmentSubstitute( schemaName );

    if ( databaseMeta != null ) {
      Database db = new Database( loggingObject, databaseMeta );
      try {
        db.connect();

        if ( !Utils.isEmpty( realTableName ) ) {
          // Check if this table exists...
          if ( db.checkTableExists( realSchemaName, realTableName ) ) {
            return db.getTableFieldsMeta( realSchemaName, realTableName );
          } else {
            throw new HopException( BaseMessages.getString(
              PKG, "SynchronizeAfterMergeMeta.Exception.TableNotFound" ) );
          }
        } else {
          throw new HopException( BaseMessages.getString(
            PKG, "SynchronizeAfterMergeMeta.Exception.TableNotSpecified" ) );
        }
      } catch ( Exception e ) {
        throw new HopException( BaseMessages.getString(
          PKG, "SynchronizeAfterMergeMeta.Exception.ErrorGettingFields" ), e );
      } finally {
        db.disconnect();
      }
    } else {
      throw new HopException( BaseMessages.getString(
        PKG, "SynchronizeAfterMergeMeta.Exception.ConnectionNotDefined" ) );
    }

  }

  /**
   * @return the schemaName
   */
  public String getSchemaName() {
    return schemaName;
  }

  /**
   * @param schemaName the schemaName to set
   */
  public void setSchemaName( String schemaName ) {
    this.schemaName = schemaName;
  }

  public boolean supportsErrorHandling() {
    return true;
  }

  private String[] normalizeAllocation( String[] oldAllocation, int length ) {
    String[] newAllocation = null;
    if ( oldAllocation.length < length ) {
      newAllocation = new String[ length ];
      System.arraycopy( oldAllocation, 0, newAllocation, 0, oldAllocation.length );
    } else {
      newAllocation = oldAllocation;
    }
    return newAllocation;
  }

  private Boolean[] normalizeAllocation( Boolean[] oldAllocation, int length ) {
    Boolean[] newAllocation = null;
    if ( oldAllocation.length < length ) {
      newAllocation = new Boolean[ length ];
      System.arraycopy( oldAllocation, 0, newAllocation, 0, oldAllocation.length );
      for ( int i = 0; i < length; i++ ) {
        if ( newAllocation[ i ] == null ) {
          newAllocation[ i ] = Boolean.TRUE; // Set based on default in setDefault
        }
      }
    } else {
      newAllocation = oldAllocation;
    }
    return newAllocation;
  }

  /**
   * If we use injection we can have different arrays lengths.
   * We need synchronize them for consistency behavior with UI
   */
  @AfterInjection
  public void afterInjectionSynchronization() {
    normalizeAllocationFields();
  }


}