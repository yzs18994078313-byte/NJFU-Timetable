(function(){
  var result={scheme:'',term:''};
  function query(url,key){var m=new RegExp('[?&]'+key+'=([^&#]+)').exec(url);return m?decodeURIComponent(m[1].replace(/\+/g,' ')):'';}
  function inspect(w,depth){
    if(depth>5)return;
    try{
      var resources=w.performance&&w.performance.getEntriesByType?w.performance.getEntriesByType('resource'):[];
      for(var i=resources.length-1;i>=0;i--){var url=resources[i].name;if(url.indexOf('mainV_index_loadkb.htmlx')<0)continue;if(!result.scheme)result.scheme=query(url,'sjmsValue');if(!result.term)result.term=query(url,'xnxqid');}
      var names=[['scheme','sjmsValue'],['term','xnxqid']];
      for(var k=0;k<names.length;k++){var key=names[k][0],name=names[k][1];if(result[key])continue;var nodes=w.document.querySelectorAll('[name="'+name+'"],#'+name);for(i=0;i<nodes.length;i++)if(nodes[i].value){result[key]=String(nodes[i].value);break;}if(!result[key]&&(typeof w[name]==='string'||typeof w[name]==='number'))result[key]=String(w[name]);}
      for(i=0;i<w.frames.length;i++)inspect(w.frames[i],depth+1);
    }catch(e){}
  }
  inspect(window,0);return JSON.stringify(result);
})()
