(function(){
  var pages=[],visited=[];
  function inspect(w,depth){if(depth>5||visited.indexOf(w)>=0)return;visited.push(w);try{if(w.location.hostname==='jwxt.njfu.edu.cn'&&w.location.pathname.indexOf('/jsxsd/')===0&&!w.document.querySelector('input[type=password]')){var tables=w.document.querySelectorAll('table');for(var i=0;i<tables.length;i++)if(tables[i].querySelector('.item-box')){var parent=tables[i].parentElement,inside=false;while(parent){if(parent.tagName==='TABLE'){inside=true;break;}parent=parent.parentElement;}if(!inside)pages.push(tables[i].outerHTML);}}for(var f=0;f<w.frames.length;f++)inspect(w.frames[f],depth+1);}catch(e){}}
  inspect(window,0);return JSON.stringify({html:pages.join('\n')});
})()
