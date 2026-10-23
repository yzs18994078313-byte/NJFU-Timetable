(function(){
  // Only adapt an authentication form; keep the university's application pages unchanged.
  if(window.__njfuPortraitLogin){window.__njfuPortraitLogin();return;}
  function fit(){
    if(Math.min(window.innerWidth,(window.screen&&window.screen.width)||window.innerWidth)>600)return;
    var password=document.querySelector('input[type="password"]');if(!password)return;
    var form=password.form||password.parentElement;
    if(!form)return;
    var viewport=document.querySelector('meta[name="viewport"]');
    if(!viewport){viewport=document.createElement('meta');viewport.name='viewport';(document.head||document.documentElement).appendChild(viewport);}
    viewport.content='width=device-width, initial-scale=1.0, viewport-fit=cover';
    if(!document.getElementById('njfu-portrait-style')){
      var style=document.createElement('style');style.id='njfu-portrait-style';
      style.textContent='@media(max-width:600px){html,body{min-width:0!important;max-width:100%!important;overflow-y:auto!important}input,button,select{max-width:100%;box-sizing:border-box}input[type=password],input[type=text],input[type=tel],input[type=email]{font-size:16px!important;min-height:44px}}';
      (document.head||document.documentElement).appendChild(style);
    }
    var bounds=form.getBoundingClientRect(),inputBounds=password.getBoundingClientRect();
    // Repair only fixed desktop positioning that places a form outside a narrow viewport.
    if(bounds.width>window.innerWidth||bounds.left<0||bounds.right>window.innerWidth||inputBounds.right>window.innerWidth||inputBounds.left<0){
      var node=form;
      while(node&&node!==document.body&&node!==document.documentElement){
        node.style.setProperty('min-width','0','important');node.style.setProperty('max-width','100%','important');
        node.style.setProperty('box-sizing','border-box','important');node.style.setProperty('margin-left','0','important');
        node.style.setProperty('left','auto','important');node.style.setProperty('right','auto','important');node.style.setProperty('transform','none','important');
        var position=window.getComputedStyle(node).position;if(position==='absolute'||position==='fixed'){node.style.setProperty('position','relative','important');node.style.setProperty('top','auto','important');}
        if(node===form){node.style.setProperty('width','100%','important');node.style.setProperty('padding','20px 16px','important');node.style.setProperty('z-index','2','important');}
        else if(node.getBoundingClientRect().width>window.innerWidth)node.style.setProperty('width','100%','important');
        node=node.parentElement;
      }
    }
  }
  window.__njfuPortraitLogin=fit;
  document.addEventListener('focusin',function(event){var input=event.target;if(!input||!/^(INPUT|TEXTAREA|SELECT)$/.test(input.tagName))return;window.setTimeout(function(){try{input.scrollIntoView({block:'center',inline:'nearest',behavior:'smooth'});}catch(e){input.scrollIntoView(false);}},250);});
  window.addEventListener('resize',function(){fit();});
  if(window.MutationObserver)new MutationObserver(function(){if(!document.querySelector('input[type=password]'))return;fit();}).observe(document.documentElement,{childList:true,subtree:true});
  fit();
})()
