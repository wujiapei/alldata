/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import { isEmpty } from 'lodash';
import { getHiveList, getReturnList, getFormatProposalsList } from '../util';
import storage from '@dataspherestudio/shared/common/helper/storage';

const pyKeywordInfoProposals = [
  {
    label: 'False',
    documentation: 'Keywords',
    insertText: 'False',
    detail: 'Keywords',
  },
  {
    label: 'class',
    documentation: 'Keywords',
    insertText: 'class',
    detail: 'Keywords',
  },
  {
    label: 'finally',
    documentation: 'Keywords',
    insertText: 'finally',
    detail: 'Keywords',
  },
  {
    label: 'is',
    documentation: 'Keywords',
    insertText: 'is',
    detail: 'Keywords',
  },
  {
    label: 'return',
    documentation: 'Keywords',
    insertText: 'return',
    detail: 'Keywords',
  },
  {
    label: 'None',
    documentation: 'Keywords',
    insertText: 'None',
    detail: 'Keywords',
  },
  {
    label: 'continue',
    documentation: 'Keywords',
    insertText: 'continue',
    detail: 'Keywords',
  },
  {
    label: 'for',
    documentation: 'Keywords',
    insertText: 'for',
    detail: 'Keywords',
  },
  {
    label: 'lambda',
    documentation: 'Keywords',
    insertText: 'lambda',
    detail: 'Keywords',
  },
  {
    label: 'try',
    documentation: 'Keywords',
    insertText: 'try',
    detail: 'Keywords',
  },
  {
    label: 'True',
    documentation: 'Keywords',
    insertText: 'True',
    detail: 'Keywords',
  },
  {
    label: 'def',
    documentation: 'Keywords',
    insertText: 'def',
    detail: 'Keywords',
  },
  {
    label: 'from',
    documentation: 'Keywords',
    insertText: 'from',
    detail: 'Keywords',
  },
  {
    label: 'nonlocal',
    documentation: 'Keywords',
    insertText: 'nonlocal',
    detail: 'Keywords',
  },
  {
    label: 'while',
    documentation: 'Keywords',
    insertText: 'while',
    detail: 'Keywords',
  },
  {
    label: 'and',
    documentation: 'Keywords',
    insertText: 'and',
    detail: 'Keywords',
  },
  {
    label: 'del',
    documentation: 'Keywords',
    insertText: 'del',
    detail: 'Keywords',
  },
  {
    label: 'global',
    documentation: 'Keywords',
    insertText: 'global',
    detail: 'Keywords',
  },
  {
    label: 'not',
    documentation: 'Keywords',
    insertText: 'not',
    detail: 'Keywords',
  },
  {
    label: 'with',
    documentation: 'Keywords',
    insertText: 'with',
    detail: 'Keywords',
  },
  {
    label: 'as',
    documentation: 'Keywords',
    insertText: 'as',
    detail: 'Keywords',
  },
  {
    label: 'elif',
    documentation: 'Keywords',
    insertText: 'elif',
    detail: 'Keywords',
  },
  {
    label: 'if',
    documentation: 'Keywords',
    insertText: 'if',
    detail: 'Keywords',
  },
  {
    label: 'or',
    documentation: 'Keywords',
    insertText: 'or',
    detail: 'Keywords',
  },
  {
    label: 'yield',
    documentation: 'Keywords',
    insertText: 'yield',
    detail: 'Keywords',
  },
  {
    label: 'assert',
    documentation: 'Keywords',
    insertText: 'assert',
    detail: 'Keywords',
  },
  {
    label: 'else',
    documentation: 'Keywords',
    insertText: 'else',
    detail: 'Keywords',
  },
  {
    label: 'import',
    documentation: 'Keywords',
    insertText: 'import',
    detail: 'Keywords',
  },
  {
    label: 'pass',
    documentation: 'Keywords',
    insertText: 'pass',
    detail: 'Keywords',
  },
  {
    label: 'break',
    documentation: 'Keywords',
    insertText: 'break',
    detail: 'Keywords',
  },
  {
    label: 'except',
    documentation: 'Keywords',
    insertText: 'except',
    detail: 'Keywords',
  },
  {
    label: 'in',
    documentation: 'Keywords',
    insertText: 'in',
    detail: 'Keywords',
  },
  {
    label: 'raise',
    documentation: 'Keywords',
    insertText: 'raise',
    detail: 'Keywords',
  },
  {
    label: 'print',
    documentation: 'Keywords',
    insertText: 'print',
    detail: 'Keywords',
  },
  {
    label: 'exec',
    documentation: 'Keywords',
    insertText: 'exec',
    detail: 'Keywords',
  },
];

// ??????????????????
/**
 *
 const commonGrammaticalStruCture = [
     {
         label: 'drop table',
         documentation: '??????hive???',
         insertText: 'drop table if exists {table_name};',
         detail: '??????????????????',
     },
     {
         label: 'create table partitioned by',
         documentation: '??????hive?????????',
         insertText: 'create table {table_name} ({columns}) partitioned by ({partition}) row format delimited fields terminated by "," stored as orc;',
         detail: '??????????????????',
     },
     {
         label: 'create table as select',
         documentation: '??????select?????????',
         insertText: 'create table {table_name} as select',
         detail: '??????????????????',
     },
     {
         label: 'insert into table',
         documentation: '????????????????????????',
         insertText: 'insert into table {table_name} partition({partition})',
         detail: '??????????????????',
     },
     {
         label: 'insert overwrite table',
         documentation: '????????????????????????',
         insertText: 'insert overwrite table {table_name} partition({partition})',
         detail: '??????????????????',
     },
 ];
 */

const buildInVariableProposals = [
  {
    label: 'run_date',
    documentation: '????????????????????????',
    insertText: '${run_date}',
    detail: '??????????????????',
  }, {
    label: 'run_date_std',
    documentation: '?????????????????????????????????????????????????????????',
    insertText: '${run_date_std}',
    detail: '??????????????????',
  }, {
    label: 'run_month_begin',
    documentation: '????????????????????????',
    insertText: '${run_month_begin}',
    detail: '??????????????????',
  }, {
    label: 'run_month_begin_std',
    documentation: '?????????????????????????????????????????????????????????',
    insertText: '${run_month_begin_std}',
    detail: '??????????????????',
  }, {
    label: 'run_month_end',
    documentation: '???????????????????????????',
    insertText: '${run_month_end}',
    detail: '??????????????????',
  }, {
    label: 'run_month_end_std',
    documentation: '????????????????????????????????????????????????????????????',
    insertText: '${run_month_end_std}',
    detail: '??????????????????',
  },
];

let functionProposals = [];

export default {
  async register(monaco) {
    const lang = 'python';

    const pyProposals = getFormatProposalsList(monaco, pyKeywordInfoProposals, '', 'Keyword');
    const BIVPro = getFormatProposalsList(monaco, buildInVariableProposals, '', 'Variable');
    // const CGSPro = getFormatProposalsList(monaco, commonGrammaticalStruCture, '', 'Reference');

    getHiveList(monaco, lang).then((list) => {
      functionProposals = list.udfProposals;
    });

    monaco.languages.registerCompletionItemProvider('python', {
      triggerCharacters: 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._'.split(''),
      async provideCompletionItems(model, position) {

        const needRefresh = storage.get('need-refresh-proposals-python')
        if (needRefresh || isEmpty(functionProposals)) {
          const list = await getHiveList(monaco, lang)
          functionProposals = list.udfProposals;
          storage.set('need-refresh-proposals-python', false)
        }

        const textUntilPosition = model.getValueInRange({
          startLineNumber: position.lineNumber,
          startColumn: 1,
          endLineNumber: position.lineNumber,
          endColumn: position.column,
        });
        const keywordMatch = textUntilPosition.match(/([^"]*)?$/i);
        const functionMatch = textUntilPosition.match(/\s+/i);
        if (functionMatch) {
          const match = functionMatch[0].split(' ')[1];
          let proposalsList = [];
          proposalsList = functionProposals.concat(BIVPro);
          return getReturnList({
            match,
            proposals: proposalsList,
            fieldString: 'insertText',
          });
        } else if (keywordMatch) {
          const matchList = keywordMatch[0].split(' ');
          const match = matchList[matchList.length - 1];
          const proposalsList = pyProposals.concat(BIVPro);
          return getReturnList({
            match,
            proposals: proposalsList,
            fieldString: 'insertText',
            position
          }, monaco);
        }
        return [];
      },
    });
  },
};
