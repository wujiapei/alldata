/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.platform.quality.step.transform

import com.platform.quality.context.DQContext
import com.platform.quality.step.write.WriteStep

import scala.util.Try

/**
 * data frame ops transform step
 */
case class DataFrameOpsTransformStep[T <: WriteStep](
    name: String,
    inputDfName: String,
    rule: String,
    details: Map[String, Any],
    writeStepOpt: Option[T] = None,
    cache: Boolean = false)
    extends TransformStep {

  def doExecute(context: DQContext): Try[Boolean] =
    Try {
      val sparkSession = context.sparkSession
      val df = rule match {
        case DataFrameOps._fromJson => DataFrameOps.fromJson(sparkSession, inputDfName, details)
        case DataFrameOps._accuracy =>
          DataFrameOps.accuracy(sparkSession, inputDfName, context.contextId, details)
        case DataFrameOps._clear => DataFrameOps.clear(sparkSession, inputDfName, details)
        case _ => throw new Exception(s"df opr [ $rule ] not supported")
      }
      if (cache) context.dataFrameCache.cacheDataFrame(name, df)
      context.runTimeTableRegister.registerTable(name, df)
      writeStepOpt match {
        case Some(writeStep) => writeStep.execute(context)
        case None => Try(true)
      }
    }.flatten

}
