<template>
  <Form
    :label-width="100"
    label-position="left">
    <FormItem
      :label="$root.$t('message.workflow.publish.desc')">
      <Input
        :rows="4"
        type="textarea"
        v-model="comment"
        :placeholder="$root.$t('message.workflow.publish.inputDesc')"></Input>
    </FormItem>
  </Form>
</template>
<script>
import api from '@dataspherestudio/shared/common/service/api';
import storage from '@dataspherestudio/shared/common/helper/storage';

export default {
  name: 'Publish',
  props: {
    currentProjectData: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      comment: '',
    };
  },
  created() {
  },
  watch: {
    'currentProjectData.id'(val) {
      if (val) {
        this.comment = '';
      }
    }
  },
  methods: {
    publish() {
      // 发布
      this.dispatch('Project:loading', true);
      api.fetch('/dss/release', {
        projectVersionID: this.currentProjectData.latestVersion.id,
        IOType: 'PROJECT',
        comment: this.comment,
      }).then(() => {
        this.dispatch('Project:loading', false);
        // 轮询结果超过十分钟，提示超时
        let timeoutVlaue = 0;
        this.checkResult(this.currentProjectData.latestVersion.id, timeoutVlaue);
      }).catch(() => {
        this.$Message.warning(this.$root.$t('message.workflow.publish.error', { name: this.currentProjectData.name }));
        this.removePercent();
        this.dispatch('Project:getData');
      });
    },
    checkResult(id, timeoutValue) {
      const timer = setTimeout(() => {
        timeoutValue += 8000;
        api.fetch('/dss/releaseQuery', { projectVersionID: +id }, 'get').then((res) => {
          if (timeoutValue <= (10 * 60 * 1000)) {
            if (res.info.status === 'Inited' || res.info.status === 'Running') {
              clearTimeout(timer);
              this.checkResult(id, timeoutValue);
            } else if (res.info.status === 'Succeed') {
              clearTimeout(timer);
              this.$Message.success(this.$root.$t('message.publish.success', { name: this.currentProjectData.name }));
              this.removePercent();
              this.dispatch('Project:getData');
              this.$emit('updateWorkflow')
            } else if (res.info.status === 'Failed') {
              clearTimeout(timer);
              this.removePercent();
              this.dispatch('Project:getData');
              this.$Modal.error({
                title: this.$root.$t('message.workflow.publish.failed'),
                content: `<p style="word-break: break-all;">${res.info.msg}</p>`,
                width: 500,
                okText: this.$root.$t('message.workflow.publish.cancel'),
              });
            }
          } else {
            clearTimeout(timer);
            this.$Message.warning(this.$root.$t('message.workflow.publish.timeout', { name: this.currentProjectData.name }));
            this.removePercent();
            this.dispatch('Project:getData');
          }
        });
      }, 8000);
    },
    removePercent() {
      let precentList = storage.get('precentList');
      if (precentList) {
        precentList = precentList.filter((item) => {
          return item.id !== this.currentProjectData.id;
        });
        storage.set('precentList', precentList);
      }
    },
  },
};
</script>
